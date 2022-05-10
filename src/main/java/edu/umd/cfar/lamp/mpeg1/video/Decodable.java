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

import edu.columbia.ee.flavor.Bitstream;
import edu.umd.cfar.lamp.mpeg1.MpegException;

public interface Decodable extends StateParsable {
	public void decode(Bitstream bitstream, DecoderState decoderState) throws IOException, MpegException;
}