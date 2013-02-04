package com.eddygao;

import java.util.ArrayList;
import java.util.Random;

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
	Drawable back;
	private Card activeCard;
	ArrayList<Drawable> spadeImages, heartImages, diamondImages, clubImages;

	public MainView(Context context) {

		super(context);
		setOnTouchListener(this);

		mCanvasPaint = new Paint();
		mCanvasPaint.setColor(0xFF228B22); // Green background
		mCanvasPaint.setAntiAlias(false);

		loadResources();

		ArrayList<Card> undealt = new ArrayList<Card>();
		for (int i = 1; i <= 13; i++) {
			undealt.add(new Card(i, Suit.spades, null, false));
			undealt.add(new Card(i, Suit.hearts, null, false));
			undealt.add(new Card(i, Suit.diamonds, null, false));
			undealt.add(new Card(i, Suit.clubs, null, false));
		}

		decks = new ArrayList<Deck>();
		decks.add(new Deck(10, 10, mCardSize.width(), mCardSize.height(),
				deckType.waste));
		for (int i = 0; i < 4; i++) {
			decks.add(new Deck(700 + 150 * i, 10, mCardSize.width(), mCardSize
					.height(), deckType.foundation));
		}

		for (int i = 0; i < 7; i++) {
			decks.add(new Deck(10 + 180 * i, 180, mCardSize.width(), mCardSize
					.height(), deckType.tableau));
		}
		// deck[0] is waste
		// deck[1-4] is foundation
		// deck[5-11] are tableau

		Random random = new Random();
		for (int i = 1; i <= 7; i++) {
			for (int j = 0; j < i; j++) {
				Card chosen = undealt.remove(random.nextInt(undealt.size()));
				if (j == i - 1)
					chosen.setReveal(true);
				chosen.setImage(cardImage(chosen));
				decks.get(i + 4).addCard(chosen);
			}
		}
		while (undealt.size() > 0) {
			Card chosen = undealt.remove(random.nextInt(undealt.size()));
			chosen.setReveal(true);
			chosen.setImage(cardImage(chosen));

			decks.get(0).addCard(chosen);
			
		}
	}

	private void loadResources() {
		back = getResources().getDrawable(R.drawable.cardback);
		spadeImages = new ArrayList<Drawable>();

		for (int i = 1; i <= 13; i++) {
			spadeImages.add(getResources().getDrawable(
					getResources().getIdentifier("spade" + i, "drawable",
							"com.eddygao.mysolitaire")));
		}
		heartImages = new ArrayList<Drawable>();

		for (int i = 1; i <= 13; i++) {
			heartImages.add(getResources().getDrawable(
					getResources().getIdentifier("heart" + i, "drawable",
							"com.eddygao.mysolitaire")));
		}
		diamondImages = new ArrayList<Drawable>();

		for (int i = 1; i <= 13; i++) {
			diamondImages.add(getResources().getDrawable(
					getResources().getIdentifier("diamond" + i, "drawable",
							"com.eddygao.mysolitaire")));
		}
		clubImages = new ArrayList<Drawable>();

		for (int i = 1; i <= 13; i++) {
			clubImages.add(getResources().getDrawable(
					getResources().getIdentifier("club" + i, "drawable",
							"com.eddygao.mysolitaire")));
		}
	}

	public Drawable cardImage(Card card) {
		if (!card.isRevealed())
			return back;
		if (card.getSuit() == Suit.spades)
			return spadeImages.get(card.getValue() - 1);
		if (card.getSuit() == Suit.hearts)
			return heartImages.get(card.getValue() - 1);
		if (card.getSuit() == Suit.diamonds)
			return diamondImages.get(card.getValue() - 1);
		if (card.getSuit() == Suit.clubs)
			return clubImages.get(card.getValue() - 1);
		return null;
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
				if (parent.getCard(i).getValue() <= card.getValue()
						&& parent.getCard(i).isRevealed())
					parent.getCard(i).moveByDelta((int) (newX - oldX),
							(int) (newY - oldY));
			}
		} else
			card.moveByDelta((int) (newX - oldX), (int) (newY - oldY));
		oldX = newX;
		oldY = newY;
		invalidate();
	}

	public boolean legalMove(Card card, Deck dest, boolean singleMove) {
		if (dest.getType() == deckType.waste)
			return false;
		else if (dest.getType() == deckType.foundation) {
			if (!singleMove) {
				return false;
			}
			if (dest.isEmpty()) {
				if (card.getValue() != 1)
					return false;
				else return true;
			} else if (card.getSuit() != dest.topDeck().getSuit()) {
				Log.v("out","uh oh");
				return false;
			}

		} else {

			if (!dest.topDeck().isRevealed())
				return false;
			int parity = 0;
			if (card.getSuit() == Suit.hearts
					|| card.getSuit() == Suit.diamonds)
				parity += 1;
			if (dest.topDeck().getSuit() == Suit.hearts
					|| dest.topDeck().getSuit() == Suit.diamonds)
				parity += 1;
			if (parity != 1)
				return false;
			if (dest.isEmpty()) {
				if (card.getValue() != 13) 
					return false;
				else return true;
			}
		}
		return (card.getValue() - dest.getCard(dest.getSize() - 1).getValue() == -1);

	}

	public void moveStackToDeck(Deck parent, Deck dest, Card card) {
		ArrayList<Card> toMove = new ArrayList<Card>();

		for (int i = 0; i < parent.getSize(); i++) {
			Card c = parent.getCard(i);
			if (c.getValue() < card.getValue() && c.isRevealed()) {
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
		boolean singleMove = (card == parent.topDeck());
		parent.removeCard(card);
		Card position = cardUnderTouch(x, y);

		if (position != null && legalMove(card, position.getParent(), singleMove)) {

			if (parent.getType() == deckType.tableau)
				moveStackToDeck(parent, position.getParent(), card);
			else
				position.getParent().addCard(card);
		} else {
			Deck deckPosition = deckUnderTouch(x, y);
			if (null != deckPosition && legalMove(card, deckPosition, singleMove)) {

				if (parent.getType() == deckType.tableau)
					moveStackToDeck(parent, deckPosition, card);
				else
					deckPosition.addCard(card);
			} else {

				if (parent.getType() == deckType.tableau)
					moveStackToDeck(parent, parent, card);
				else
					parent.addCard(card);
			}

		}
		invalidate();
	}

}