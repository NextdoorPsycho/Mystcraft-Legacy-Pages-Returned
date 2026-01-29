package art.arcane.mystcraft.instability;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

/**
 * Represents a deck of instability effect cards.
 * Cards are drawn in order based on instability level.
 */
public class Deck {

  private final String name;
  private final LinkedList<String> cards;

  /**
   * Creates a new deck with the given name and cards.
   *
   * @param name  The deck name (e.g., "basic", "harsh", "destructive")
   * @param cards The initial cards in the deck
   */
  public Deck(String name, Collection<String> cards) {
    this.name = name;
    this.cards = new LinkedList<>(cards);
  }

  /**
   * Gets the deck name.
   */
  public String getName() {
    return name;
  }

  /**
   * Gets an unmodifiable view of the cards in this deck.
   */
  public Collection<String> getCards() {
    return Collections.unmodifiableCollection(cards);
  }

  /**
   * Gets the number of cards in this deck.
   */
  public int size() {
    return cards.size();
  }

  /**
   * Checks if this deck is empty.
   */
  public boolean isEmpty() {
    return cards.isEmpty();
  }

  /**
   * Removes all cards from this deck.
   */
  public void removeAll() {
    cards.clear();
  }

  /**
   * Shuffles the deck using the given random source.
   *
   * @param random The random number generator
   */
  public void shuffle(Random random) {
    Collections.shuffle(cards, random);
  }

  /**
   * Draws the top card from the deck.
   *
   * @return The drawn card, or null if the deck is empty
   */
  public String draw() {
    if (cards.isEmpty()) {
      return null;
    }
    return cards.removeFirst();
  }

  /**
   * Puts a card on the bottom of the deck.
   *
   * @param card The card to add
   */
  public void putOnBottom(String card) {
    cards.addLast(card);
  }

  /**
   * Puts a card on top of the deck.
   *
   * @param card The card to add
   */
  public void putOnTop(String card) {
    cards.addFirst(card);
  }

  /**
   * Transfers all cards from another deck to the bottom of this deck.
   * The other deck is cleared after transfer.
   *
   * @param other The deck to transfer from
   */
  public void putOnBottom(Deck other) {
    cards.addAll(other.cards);
    other.cards.clear();
  }

  /**
   * Checks if the deck contains a specific card.
   *
   * @param card The card to check for
   * @return true if the card is in the deck
   */
  public boolean contains(String card) {
    return cards.contains(card);
  }

  /**
   * Creates a copy of this deck.
   *
   * @return A new deck with the same name and cards
   */
  public Deck copy() {
    return new Deck(name, new LinkedList<>(cards));
  }

  @Override
  public String toString() {
    return "Deck{" + name + ", cards=" + cards.size() + "}";
  }
}
