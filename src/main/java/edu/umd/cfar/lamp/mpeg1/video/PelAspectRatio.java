/***************************************
 *            ViPER-MPEG               *
 *  The Video Processing               *
 *         Evaluation Resource         *
 *             MPEG-1 Decoder          *
 * Distributed under the LGPL license  *
 *        Terms available at gnu.org.  *
 *                                     *
 *  Copyright University of Maryland,  *
 *                      College Park.  *
 ***************************************/
package edu.umd.cfar.lamp.mpeg1.video;

import java.io.IOException;
import java.math.BigDecimal;

import edu.columbia.ee.flavor.Bitstream;
import edu.umd.cfar.lamp.mpeg1.Parsable;
import edu.umd.cfar.lamp.mpeg1.ParsingException;

/**
 * Parses and interprets the pixel aspect ratio of an MPEG-1 file. For more
 * information, see the MPEG-1 spec or http://www.mir.com/DMG/aspect.html .
 */
public class PelAspectRatio implements Parsable {
	private int value = 0;

	private static final int[][] EXACT_PEL_RATIOS = {
			{ 0, 0 }, { 1, 1 }, { 6735, 10000 },
			{ 7031, 10000 }, { 7615, 10000 },
			{ 8055, 10000 }, { 8437, 10000 },
			{ 8935, 10000 }, { 54, 59 },
			{ 9815, 10000 }, { 10255, 10000 },
			{ 10695, 10000 }, { 11, 10 },
			{ 11575, 10000 }, { 12015, 10000 } };
	private static final BigDecimal[] BIG_PEL_RATIOS = new BigDecimal[EXACT_PEL_RATIOS.length];
	private static final float[] PEL_RATIOS = { Float.NaN, 1f, 0.6735f, 0.7031f,
			0.7615f, 0.8055f, 0.8437f, 0.8935f, 0.9157f, 0.9815f, 1.0255f,
			1.0695f, 1.095f, 1.1575f, 1.2015f };

	static {
		BIG_PEL_RATIOS[0] = BigDecimal.ZERO;
	}

	public PelAspectRatio() {
	}

	public PelAspectRatio(int i) {
		try {
			setValue(i);
		} catch (ParsingException e) {
			throw new IllegalArgumentException("Not a valid aspect ratio index: " + i);
		}
	}

	@Override
	public void parse(Bitstream bitstream) throws IOException {
		if (value > 0) {
			throw new IllegalStateException("Already parsed!");
		}
		int s = bitstream.nextbits(4);
		setValue(s);
		bitstream.skipbits(4);
	}

	/**
	 * @param newValue
	 * @throws ParsingException
	 */
	private void setValue(int newValue) throws ParsingException {
		if (newValue < PEL_RATIOS.length) {
			if (newValue == 0) {
				throw new ParsingException(
						"Value 0 for PelAspectRatio forbidden.");
			} else {
				value = newValue;
			}
		} else {
			throw new ParsingException("Value " + newValue
					+ " for PelAspectRatio reserved.");
		}
	}

	public float getFloatValue() {
		return PEL_RATIOS[value];
	}

	public int getNumerator() {
		return EXACT_PEL_RATIOS[value][0];
	}

	public int getDenominator() {
		return EXACT_PEL_RATIOS[value][1];
	}

	public BigDecimal getBigDecimalValue() {
		BigDecimal d = BIG_PEL_RATIOS[value];
		if (d == null) {
			d = new BigDecimal(getNumerator()).divide(new BigDecimal(getDenominator()));
			BIG_PEL_RATIOS[value] = d;
		}
		return d;
	}

	/**
	 * Gets the index of the pel aspect ratio.
	 * 
	 * @return Returns the ratio index.
	 */
	public int getValue() {
		return value;
	}
}