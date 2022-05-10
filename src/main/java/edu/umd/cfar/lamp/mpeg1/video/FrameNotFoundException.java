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

import edu.umd.cfar.lamp.mpeg1.MpegException;

public class FrameNotFoundException extends MpegException {
	public FrameNotFoundException() {
	}

	public FrameNotFoundException(String message) {
		super(message);
	}
}
