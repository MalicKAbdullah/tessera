package com.malickabdullah.tessera.designs.note

import java.time.LocalDate

/** [source] names the work and, where the wording is a translation, the public-domain translator. */
data class Quote(val text: String, val author: String, val source: String)

/**
 * Bundled quotations for the daily-quote design. Every author died before 1900
 * or the wording is from a translation or edition that is itself in the public
 * domain, so nothing here is under copyright.
 */
object Quotes {
    val all: List<Quote> = listOf(
        Quote("Simplify, simplify.", "Henry David Thoreau", "Walden, 1854"),
        Quote("Nothing can bring you peace but yourself.", "Ralph Waldo Emerson", "Self-Reliance, 1841"),
        Quote("Well done is better than well said.", "Benjamin Franklin", "Poor Richard's Almanack, 1737"),
        Quote("Hold every hour in your grasp.", "Seneca", "Letters to Lucilius, I, tr. R. M. Gummere, 1917"),
        Quote("The mind is its own place, and in itself can make a heaven of hell, a hell of heaven.", "John Milton", "Paradise Lost, 1667"),
        Quote("The best way of avenging thyself is not to become like the wrong-doer.", "Marcus Aurelius", "Meditations, VI, tr. George Long, 1862"),
        Quote("To strive, to seek, to find, and not to yield.", "Alfred, Lord Tennyson", "Ulysses, 1842"),
        Quote("Time is but the stream I go a-fishing in.", "Henry David Thoreau", "Walden, 1854"),
        Quote("To be great is to be misunderstood.", "Ralph Waldo Emerson", "Self-Reliance, 1841"),
        Quote("Lost time is never found again.", "Benjamin Franklin", "Poor Richard's Almanack, 1748"),
        Quote("To see a world in a grain of sand, and a heaven in a wild flower.", "William Blake", "Auguries of Innocence, c. 1803"),
        Quote("The superior man is modest in his speech, but exceeds in his actions.", "Confucius", "Analects, XIV, tr. James Legge, 1861"),
        Quote("I wished to live deliberately, to front only the essential facts of life.", "Henry David Thoreau", "Walden, 1854"),
        Quote("Nothing great was ever achieved without enthusiasm.", "Ralph Waldo Emerson", "Circles, 1841"),
        Quote("Men are disturbed, not by things, but by the principles and notions which they form concerning things.", "Epictetus", "Enchiridion, tr. Elizabeth Carter, 1758"),
        Quote("A thing of beauty is a joy for ever.", "John Keats", "Endymion, 1818"),
        Quote("Reading maketh a full man; conference a ready man; and writing an exact man.", "Francis Bacon", "Of Studies, 1625"),
        Quote("There is no charm equal to tenderness of heart.", "Jane Austen", "Emma, 1815"),
        Quote("I am large, I contain multitudes.", "Walt Whitman", "Song of Myself, 1855"),
        Quote("To see what is right and not to do it is want of courage.", "Confucius", "Analects, II, tr. James Legge, 1861"),
        Quote("How many a man has dated a new era in his life from the reading of a book.", "Henry David Thoreau", "Walden, 1854"),
        Quote("It is easy to be brave from a safe distance.", "Aesop", "Fables, tr. George Fyler Townsend, 1867"),
        Quote("It was the best of times, it was the worst of times.", "Charles Dickens", "A Tale of Two Cities, 1859"),
        Quote("Trust thyself: every heart vibrates to that iron string.", "Ralph Waldo Emerson", "Self-Reliance, 1841"),
        Quote("We know what we are, but know not what we may be.", "William Shakespeare", "Hamlet, IV.v, c. 1600"),
        Quote("To thine own self be true.", "William Shakespeare", "Hamlet, I.iii, c. 1600"),
        Quote("Early to bed and early to rise, makes a man healthy, wealthy, and wise.", "Benjamin Franklin", "Poor Richard's Almanack, 1735"),
        Quote("The mass of men lead lives of quiet desperation.", "Henry David Thoreau", "Walden, 1854"),
        Quote("All the world's a stage, and all the men and women merely players.", "William Shakespeare", "As You Like It, II.vii, c. 1599"),
        Quote("That government of the people, by the people, for the people, shall not perish from the earth.", "Abraham Lincoln", "Gettysburg Address, 1863"),
    )

    /** The quote for [date]: one per calendar day, cycling through the whole set. */
    fun of(date: LocalDate): Quote = all[Math.floorMod(date.toEpochDay(), all.size.toLong()).toInt()]
}
