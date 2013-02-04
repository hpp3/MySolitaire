package com.eddygao;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.eddygao.Card.Suit;
import com.eddygao.Deck.deckType;
import com.eddygao.mysolitaire.R;

public class MainView extends View implements OnTouchListener {
	private Paint mCanvasPaint;

	private Bitmap mCacheBitmap;
	private boolean mUseCache = false;

	private Rect mScreenSize = new Rect();

	private Rect mCardSize = new Rect();
	private int cardXCap;
	private int cardYCap;
	private int mCardCap;
	private ArrayList<Deck> decks;
	private float oldX, oldY;

	private Card activeCard;
	ArrayList<Drawable> spadeImages;

	public MainView(Context context) {

		super(context);
		setOnTouchListener(this);

		mCanvasPaint = new Paint();
		mCanvasPaint.setColor(0xFF228B22); // Green background
		mCanvasPaint.setAntiAlias(false);

		Drawable back = getResources().getDrawable(R.drawable.cardback);

		spadeImages = new ArrayList<Drawable>();

		for (int i = 1; i <= 13; i++) {
			spadeImages.add(getResources().getDrawable(
					getResources().getIdentifier("spade" + i, "drawable",
							"com.eddygao.mysolitaire")));
		}

		decks = new ArrayList<Deck>();
		decks.add(new Deck(10, 10, mCardSize.width(), mCardSize.height(),
				deckType.tableau));

		for (int i = 13; i > 0; i--) {
			decks.get(0).addCard(
					new Card(i, Suit.spades, spadeImages.get(i - 1), decks
							.get(0), true));
		}

		decks.add(new Deck(300, 10, mCardSize.width(), mCardSize.height(),
				deckType.foundation));
		decks.get(1)
				.addCard(new Card(10, Suit.spades, back, decks.get(1), true));
		decks.add(new Deck(600, 10, mCardSize.width(), mCardSize.height(),
				deckType.waste));
		decks.add(new Deck(900, 10, mCardSize.width(), mCardSize.height(),
				deckType.tableau));
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		// Store current screen size
		mScreenSize.set(0, 0, w, h);
		// Log.v("size ", mScreenSize.toString());
		// Calculate card and decks sizes and positions
		int cw = w / 15;
		mCardSize.set(0, 0, cw, (int) (cw * 1.5));

		int freeSize = w - cw * 7;
		mCardCap = freeSize / (6 + 4 * 2);

		int cy = (int) (mScreenSize.height() * 0.35);

		for (Deck curDeck : decks) {
			curDeck.resize(mCardSize.width(), mCardSize.height());
		}
		invalidate();
	}

	@Override
	public void onDraw(Canvas canvas) {

		// Cache?
		if (mUseCache) {
			// Yes
			canvas.drawBitmap(mCacheBitmap, 0, 0, null);
		} else {
			// No
			mCanvasPaint.setStyle(Style.FILL);
			canvas.drawRect(mScreenSize, mCanvasPaint);
			// Draw decks
			for (Deck curDeck : decks) {
				curDeck.draw(canvas);
			}
		}
	}

	public Card cardUnderTouch(float x, float y) {
		for (Deck deck : decks) {
			for (int i = deck.getSize() - 1; i >= 0; i--) {
				Card card = deck.getCard(i);
				if (card.inCard(x, y)) {
					return card;
				}
			}
		}
		return null;
	}

	public Deck deckUnderTouch(float x, float y) {
		for (Deck deck : decks) {
			if (x > deck.getX() && x < deck.getWidth() + deck.getX()
					&& y > deck.getY() && y < deck.getHeight() + deck.getY()) {
				return deck;
			}
		}
		return null;
	}

	@Override
	public boolean onTouch(View v, MotionEvent e) {
		int action = e.getAction();
		if (action == MotionEvent.ACTION_DOWN) {
			oldX = e.getX();
			oldY = e.getY();
			Card card = cardUnderTouch(e.getX(), e.getY());
			if (card != null) {
				activeCard = card;
			}
		}
		if (action == MotionEvent.ACTION_MOVE) {
			if (activeCard != null) {
				moveCard(activeCard, e.getX(), e.getY());

			}
		}
		if (action == MotionEvent.ACTION_UP) {
			if (null != activeCard)
				cardReleased(activeCard, e.getX(), e.getY());
			activeCard = null;
		}

		return true;
	}

	public void moveCard(Card card, float newX, float newY) {
		Deck parent = card.getParent();
		if (parent.getType() == deckType.tableau) {
			for (int i = 0; i < parent.getSize(); i++) {
				if (parent.getCard(i).getValue() <= card.getValue())
					parent.getCard(i).moveByDelta((int) (newX - oldX),
							(int) (newY - oldY));
			}
		} else
			card.moveByDelta((int) (newX - oldX), (int) (newY - oldY));
		oldX = newX;
		oldY = newY;
		invalidate();
	}

	public boolean legalMove(Card card, Deck dest) {
		if (dest.getType() == deckType.waste) return false;
		else if (dest.getType() == deckType.foundation) {
			if (card.getParent().topDeck() != card) return false;
			if (card.getSuit() != card.getParent().topDeck().getSuit()) return false;
		}
		else {
			int parity = 0;
			if (card.getSuit() == Suit.hearts || card.getSuit() == Suit.diamonds) parity += 1;
			if (card.getParent().topDeck().getSuit() == Suit.hearts || card.getParent().topDeck().getSuit() == Suit.diamonds) parity += 1;
			if (parity != 1) return false;
		}
		return (card.getValue() - dest.getCard(dest.getSize()-1).getValue() == -1);

	}

	public void moveStackToDeck(Deck parent, Deck dest, Card card) {
		ArrayList<Card> toMove = new ArrayList<Card>();

		for (int i = 0; i < parent.getSize(); i++) {
			Card c = parent.getCard(i);
			if (c.getValue() < card.getValue()) {
				toMove.add(c);
			}
		}
		for (Card c : toMove) {
			parent.removeCard(c);
		}
		dest.addCard(card);
		for (Card c : toMove) {
			dest.addCard(c);
		}
	}

	public void cardReleased(Card card, float x, float y) {
		Deck parent = card.getParent();
		parent.removeCard(card);
		Card position = cardUnderTouch(x, y);

		if (position != null && legalMove(card, position.getParent())) {
			
			if (parent.getType() == deckType.tableau)
				moveStackToDeck(parent, position.getParent(), card);
			else 
				position.getParent().addCard(card);
		} else {
			Deck deckPosition = deckUnderTouch(x, y);
			if (null != deckPosition) {
				
				if (parent.getType() == deckType.tableau)
					moveStackToDeck(parent, deckPosition, card);
				else 
					deckPosition.addCard(card);
			} else {
				
				if (parent.getType() == deckType.tableau)
					moveStackToDeck(parent, parent, card);
				else parent.addCard(card);
			}

		}
		invalidate();
	}

}