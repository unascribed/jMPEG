# jMPEG

[Original project page](http://viper-toolkit.sourceforge.net/products/jmpeg/)

This is a pure Java MPEG-1 decoder with the capability to seek to exact frame
indexes, originally part of the [ViPER](http://viper-toolkit.sourceforge.net/)
project. The code is from 2003, and while it works on modern Java, it emits
various warnings and could benefit from some API cleanup. That's what this is.

## Why are you updating this?
Patents on MPEG-1 have long since expired, and it's a relatively low-complexity
codec that can be decoded with a relatively small and fast decoder. jMPEG is
slow by 2003 standards, but the JVM JIT is a lot better nowadays and, well,
computers are faster.

MPEG-1 isn't the best codec out there anymore, but for short or small videos,
it does the job more than well enough. jMPEG allows you to add animation
capability to a Java program without pulling in native dependencies, and without
resorting to image sequences or animated GIFs - you get to use a real video
codec, with motion estimation and proper inter-frames. The 120k JAR that makes
up jMPEG is worth the space you'll save.

I originally dusted this off for my own fork of SplashAnimation, a Minecraft
mod. It used PNG image sequences, and the animation I wanted was a whopping 50
megabytes in that format. Via MPEG-1, the quality is essentially the same, and
the file is a cool 8 megabytes.

## Changes
- Fully separated from ViPER
- Introduced Gradle build system
- Updated code style
- Changed Image return value in getImage to BufferedImage
- Introduced getRawPixels API in VideoDecoder to avoid AWT
- Removed dependency on ViPER's Rational class
- Introduced generics
- Removed usage of legacy collections like Vector
- Replaced usage deprecated primitive wrapper constructors with valueOf

## Licensing
jMPEG is under the LGPLv2.1, available in this repo as COPYING. It depends
on bits and pieces of the Flavor framework, which is under a custom "Flavor
Artistic License", reproduced in this repo as COPYING.flavor.

The original jMPEG depended on a single `Rational` class from the main ViPER
code, which is under the GPLv2. I replaced it with usage of Java's built-in
BigDecimal. It's only used by pixel aspect ratio code, which is only exposed in
the API and not actually used by the jMPEG code, and any modern usage of MPEG-1
is probably going to have square pixels anyway.

## Alright, how do I use it

```gradle
repositories {
	maven {
		url 'https://repo.sleeping.town'
		content {
			includeGroup 'com.unascribed'
		}
	}
}

dependencies {
	implementation 'com.unascribed:jmpeg:5.0'
}
```

The first release is 5.0, as I'm picking up where ViPER's versioning left off.

The entrypoint to the API is `Mpeg1File`. You'll want to retrieve the
`Mpeg1VideoStream` via `getVideoStream()` - you can then call `seek(int)` to
select a frame you want to retrieve, and `getImage()` to get the frame data.
If you can't use AWT images (e.g. you're using LWJGL3) then you can call
`getRawPixels()` to retrieve an int array containing ARGB data. You'll need to
check `getFrameWidth()` and `getFrameHeight()` to interpret the array properly.

## Wait, how do I encode MPEG-1 video?
It can take a lot of finagling; MPEG-1 is an old codec with outdated design
ideals. It predates the concept of CRF and multipass encoding modes.

Here's a decent starting point, using FFmpeg:

`ffmpeg -i src.avi -minrate 100k -bufsize 2M -maxrate 5M dst.mpg`

FFmpeg will complain about setting minrate to something other than 0; ignore it,
we do that to ensure a baseline level of quality. You can push it down to see
if you can get a smaller file without too much mud. It depends on the complexity
of your input. As with most video encoding, this is a lot of trial and error to
get right, and there's not a lot of assistance available in the encoder for a
codec this primitive.

The relationship between bufsize and maxrate is somewhat confusing. bufsize
describes the amount of memory the decoder is expected to have, as MPEG-1 was
expected to be decoded by fixed-function hardware with very little RAM to spare.
FFmpeg will help you out with this; if you set maxrate too high or too low, it
will print warnings saying buffer over/underflow.
