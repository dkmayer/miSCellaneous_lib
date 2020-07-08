
/*
	This file is part of miSCellaneous, a program library for SuperCollider 3

	Created: 2020-07-08, version 0.24
	Copyright (C) 2009-2020 Daniel Mayer
	Email: 	daniel-mayer@email.de
	URL:	http://daniel-mayer.at

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program; if not, write to the Free Software
	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/


Fb1 : UGen {

	// with v0.22 Fb1 can be ar or kr
	// for backward compatibility the old convention of new = ar is kept

	*new { |func, in, outSize = 0, inDepth = 1, outDepth = 2, inInit, outInit,
		blockSize = 64, blockFactor = 1, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1.ar(func, in, outSize, inDepth, outDepth, inInit, outInit,
			blockSize, blockFactor, graphOrderType,
			leakDC, leakCoef)
	}

	*checkInits { |inBufs, outBufs, inInit, outInit, inDepth, outDepth|

		var initSingleBuf = { |initItem, buf|
			case
			{ initItem.isNil }{ }
			{ initItem.isKindOf(Number) }{
				buf.set([initItem], buf.numFrames-1)
			}
			{ initItem.isKindOf(SequenceableCollection) }{
				initItem.every(_.isKindOf(Number)).if {
					(initItem.size > buf.numFrames).if {
						SimpleInitError("wrong inInit/outInit data, size too large").throw
					}{
						buf.set(initItem.reverse, buf.numFrames-(initItem.size))
					}
				}{
					SimpleInitError(
						"wrong inInit/outInit data," ++
						"inconsistent array, see help"
					).throw
				}
			}
			{ true }{
				SimpleInitError(
					"wrong inInit/outInit data, " ++
					"see help for conventions"
				)
			}
		};

		var makeDepthArray = { |depth, buf|
			case
			{ depth.isKindOf(Integer) }{ (0..depth-1) }
			{ depth.isKindOf(SequenceableCollection) }{
				depth.every(_.isKindOf(Integer)).if {
					(depth.size > buf.numFrames).if {
						SimpleInitError("wrong inDepth/outDepth, size too large").throw
					}{
						depth
					}
				}{
					SimpleInitError(
						"wrong inDepth/outDepth data," ++
						"inconsistent array, see help"
					).throw
				}
			}
			{ true }{
				SimpleInitError(
					"wrong inDepth/outDepth data, " ++
					"see help for conventions"
				)
			}
		};

		[inInit, outInit].do { |init, i|
			var buffers = [inBufs, outBufs][i];
			var ioSize = [inBufs.size, outBufs.size][i];
			init.isKindOf(SequenceableCollection).if {
				(init.size > ioSize).if {
					SimpleInitError(
						"wrong inInit/outInit data, " ++
						"size too large, for setting a number of init values " ++
						"per in/out you need double brackets - "
						"check conventions in help"
					).throw
				}{
					(init.size < ioSize).if {
						"".postln;
						("NOTE: " ++ ((i == 0).if { "inInit " }{ "outInit " }) ++
							"size smaller than " ++
							((i == 0).if { "size of in, " }{ "outSize, " }) ++
							"index wrapping applied").postln;
					};
					buffers.do { |bufs, j|
						bufs.do { |buf| initSingleBuf.(init.wrapAt(j), buf) }
					}
				}
			}{
				buffers.do { |bufs|
					bufs.do { |buf| initSingleBuf.(init, buf) }
				}
			}
		};

		^[inDepth, outDepth].collect { |depth, i|
			var buffers = [inBufs, outBufs][i];
			var ioSize = [inBufs.size, outBufs.size][i];
			depth.isKindOf(SequenceableCollection).if {
				(depth.size > ioSize).if {
					SimpleInitError(
						"wrong inDepth/outDepth data, " ++
						"size too large, for setting a number of inDepth/outDepth indices " ++
						"per buffer you need double brackets - "
						"check conventions in help"
					).throw
				}{
					(depth.size < ioSize).if {
						"".postln;
						("NOTE: " ++ ((i == 0).if { "inDepth " }{ "outDepth " }) ++
							"size smaller than " ++
							((i == 0).if { "size of in, " }{ "outSize, " }) ++
							"index wrapping applied").postln;
					};
					buffers.collect { |bufs, j|
						makeDepthArray.(depth.wrapAt(j), bufs[0])
					}
				}
			}{
				buffers.collect { |bufs| makeDepthArray.(depth, bufs[0]) }
			}
		};
	}


	*ar { |func, in, outSize = 0, inDepth = 1, outDepth = 2, inInit, outInit,
		blockSize = 64, blockFactor = 1, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		var inSize, inBufsTemp, inBufs, outBufs, iteratedOut, outType,
			bufSize, inDepthFlop, outDepthFlop, makeBlockCount, makeFeedSig,
			blockCount, bufOffset, outSig, doThis, orderSym;

		// some helper funtions:
		// we want be able to handle signals of size 0 as resp. within 'in' and 'out',
		// but for data processing it's easier have all in unified nested arrays.
		// Preparations are a bit lengthy, but core bufrd/wr loop is short.

		// makeBufs encodes into unified nesting
		var makeBufs_0 = { |num, bufSize| { LocalBuf(bufSize).clear } ! max(1, num) };
		var makeBufs = { |num, bufSize|
			num.isKindOf(Integer).if {
				max(1, num).collect { makeBufs_0.(1, bufSize) }
			}{
				num.collect(makeBufs_0.(_, bufSize))
			}
		};

		var getOutType = { |outSize|
			outSize.isNumber.if {
				(outSize == 0).if { 0 }{ 1 }
			}{
				2
			}
		};

		var outDispatch = { |sig, i, j, type|
			(type == 0).if {
				sig
			}{
				(type == 1).if {
					sig[i]
				}{
					(sig[i].size == 0).if { sig[i] }{ sig[i][j] }
				}
			}
		};

		// indexDispatch and shapeSig decode from unified nesting
		var indexDispatch = { |sig, i, j|
			case
				{ sig.size == 0 }{ sig }
				{ sig[i].size == 0 }{ sig[i] }
				{ true }{ sig[i][j] }
		};

		var shapeSig = { |sig, sizes|
			sizes.isKindOf(SequenceableCollection).if {
				sizes.collect { |size, j|
					(size == 0).if { sig[j][0] }{ sig[j] }
				}
			}{
				(sizes == 0).if { sig[0][0] }{ sig.collect(_[0]) }
			}
		};

		// nils in the depth matrix indicate positions to insert zeros.
		// This can save many ugens in the case of multichannel feedback/feedforward
		// with differentiated lookback size.

		var fillWithNils = { |array|
			var maxSize = array.collect(_.size).maxItem;
			array.collect(_.extend(maxSize, nil))
		};

		// lookback beyond blockSize

		// basically all the same in the core procedure below,
		// but the buffer indices to be used need a looping offset

		makeBlockCount = {
			Main.versionAtLeast(3, 9).if {
				Duty.kr(ControlDur.ir, 0, Dseq((0..blockFactor-1), inf))
			}{
				// correct init bug for older SC version
				Duty.kr(ControlDur.ir, 0, Dseq((0..blockFactor-1), inf) - 1 % blockFactor)
			}
		};
		blockCount = (blockFactor > 1).if { makeBlockCount.() }{ 0 };
		bufOffset = blockCount * blockSize;

		inSize = in.isKindOf(SequenceableCollection).if { in.collect(_.size) }{ in.size };
		bufSize = blockSize * blockFactor;

		inBufsTemp = makeBufs.(inSize, bufSize);
		inBufs = makeBufs.(inSize, bufSize);
		outBufs = makeBufs.(outSize, bufSize);
		outType = getOutType.(outSize);

		#inDepth, outDepth = this.checkInits(inBufs, outBufs, inInit, outInit, inDepth, outDepth);

		// depths were passed per signal, but we rather want to iterate over lookback indices

		inDepthFlop = fillWithNils.(inDepth).flop;
		outDepthFlop = fillWithNils.(outDepth).flop;


		// trick for buffering feedforward audio signals:
		// store in temporary buffers first and write with kr to 'real' buffers thereafter.
		// That way we can use the 'real' buffers in the iteration process below,
		// which is based on the order of the synthdef graph.

		// the temporary buffers cannot be used for reference to feedforward data in the
		// iteration over blockSize, as they are overriden immediately.

		// same structure to bring encoded feedback and feedforward signals
		// to desired format for func's 'in' and 'out'

		makeFeedSig = { |depthFlop, bufs, sizes, i|
			var sig;
			depthFlop.collect { |depths, j|
				sig = bufs.collect { |buf, k|
					depths[k].isNil.if { [0] }{
						BufRd.kr(1, buf, (i-depths[k]) % bufSize)
					}
				};
				shapeSig.(sig, sizes);
			}
		};


		// begin of core definition
		// 3 versions of implementation

		// with graphOrderType = 1 (default), buffer reading and writing is forced
		// to its desired order with the use of sum and <!

		// with graphOrderType = 2, buffer reading and writing is forced
		// to its desired order with the use of iterated <! operators

		// graphOrderType = 0:
		// as all my test examples worked the same without forced graph ordering,
		// I provide this implementation too as it saves a lot of ugens


		case
		{ (graphOrderType == 1) or: { graphOrderType == 2 } }{

			// default implementation: graphOrderType == 1
			orderSym = (graphOrderType == 1).if { '+' }{ '<!' };

			doThis = DC.ar(0);

			// write ar 'in' signals to buffers (happens per block)
			inBufsTemp.do { |bufs, i|
				bufs.do { |buf, j|
					doThis = orderSym.applyTo(
						doThis,
						BufWr.ar(indexDispatch.(in, i, j), buf, Phasor.ar(0, 1, 0, bufSize))
					)
				}
			};
			doThis = A2K.kr(doThis);

			// now build the iteration into the graph
			blockSize.do { |i|
				// bufOffset for blockFactor > 1
				var l = bufOffset + i;

				// prepare data from 'in' signals
				// force writers after temporary writer
				inBufs.do { |bufs, j|
					bufs.do { |buf, k|
						doThis = orderSym.applyTo(
							doThis,
							BufWr.kr(BufRd.kr(1, inBufsTemp[j][k], l), buf, l)
						)
					}
				};

				// here comes actual fb/ff relation into play
				// it can refer to former 'in'/'out' values (see makeFeedSig definition)

				iteratedOut = func.value(
					makeFeedSig.(inDepthFlop, inBufs, inSize, l),
					makeFeedSig.(outDepthFlop, outBufs, outSize, l),
					i
				);

				// the calculated next sample(s) written to buffer(s)
				outBufs.do { |bufs, j|
					bufs.do { |buf, k|
						doThis = orderSym.applyTo(
							doThis,
							BufWr.kr(outDispatch.(iteratedOut, j, k, outType), buf, l)
						)
					}
				};
			};
			// move through buffers(s), make ar signal
			// '<! doThis' - want last BufRd to be before BufWrs !
			outSig = BufRd.ar(1, shapeSig.(outBufs, outSize), Phasor.ar(0, 1, 0, bufSize) <! doThis);
		}
		{ graphOrderType == 0 }{
			// alternative implementation without forced order of
			// buffer reading and writing

			// write ar 'in' signals to buffers (happens per block)
			inBufsTemp.collect { |bufs, i|
				bufs.collect { |buf, j|
					BufWr.ar(indexDispatch.(in, i, j), buf, Phasor.ar(0, 1, 0, bufSize))
				}
			};

			// now build the iteration into the graph
			blockSize.do { |i|
				// bufOffset for blockFactor > 1
				var l = bufOffset + i;

				// prepare data from 'in' signals
				inBufs.collect { |bufs, j|
					bufs.collect { |buf, k|
						BufWr.kr(BufRd.kr(1, inBufsTemp[j][k], l), buf, l)
					}
				};
				// here comes actual fb/ff relation into play
				// it can refer to former 'in'/'out' values (see makeFeedSig definition)
				iteratedOut = func.value(
					makeFeedSig.(inDepthFlop, inBufs, inSize, l),
					makeFeedSig.(outDepthFlop, outBufs, outSize, l),
					i
				);
				// the calculated next sample(s) written to buffer(s)
				outBufs.collect { |bufs, j|
					bufs.collect { |buf, k|
						BufWr.kr(outDispatch.(iteratedOut, j, k, outType), buf, l)
					}
				}
			};
			// move through buffers(s), make ar signal
			outSig = BufRd.ar(1, shapeSig.(outBufs, outSize), Phasor.ar(0, 1, 0, bufSize));
		}

		{ true }{ SimpleInitError("wrong graphOrderType, must be 0, 1 or 2, see help").throw };

		^leakDC.if { LeakDC.ar(outSig, leakCoef) }{ outSig }
	}


// The kr variant provides a nearly identical interface,
// but without args blockSize and blockFactor,
// therefore the implementation, although much more straight,
// has subtle differences:
// buffer sizes are taken from depth params,
// in samples don't have to be buffered temporarily
// instead inBufs have to be rewritten,
// also some helper functions are slightly different.


*kr { |func, in, outSize = 0, inDepth = 1, outDepth = 2, inInit, outInit,
		graphOrderType = 1, leakDC = true, leakCoef = 0.995|

		var inSize, inBufs, outBufs, iteratedOut, outType, unshapedOut,
			inDepthFlop, outDepthFlop, makeFeedSig, blockCount, shapedOutSig,
			outSig, doThis, orderSym;

		// some helper funtions:
		// we want be able to handle signals of size 0 as resp. within 'in' and 'out',
		// but for data processing it's easier have all in unified nested arrays.
		// Preparations are a bit lengthy, but core bufrd/wr loop is short.

		// makeBufs encodes into unified nesting
		var makeBufs_0 = { |num, bufSize| { LocalBuf(bufSize).clear } ! max(1, num) };


		// different in the kr case, must take bufSizes from depth
		var makeBufs = { |num, depth|
			num.isKindOf(Integer).if {
				max(1, num).collect { |i|
					makeBufs_0.(1, depth.miSC_maybeWrapAt(i).asArray.maxItem)
				}
			}{
				num.collect { |x, i|
					makeBufs_0.(x, depth.miSC_maybeWrapAt(i).asArray.maxItem)
				}
			}
		};

		var getOutType = { |outSize|
			outSize.isNumber.if {
				(outSize == 0).if { 0 }{ 1 }
			}{
				2
			}
		};

		var outDispatch = { |sig, i, j, type|
			(type == 0).if {
				sig
			}{
				(type == 1).if {
					sig[i]
				}{
					(sig[i].size == 0).if { sig[i] }{ sig[i][j] }
				}
			}
		};

		// indexDispatch and shapeSig decode from unified nesting
		var indexDispatch = { |sig, i, j|
			case
				{ sig.size == 0 }{ sig }
				{ sig[i].size == 0 }{ sig[i] }
				{ true }{ sig[i][j] }
		};

		var shapeSig = { |sig, sizes|
			sizes.isKindOf(SequenceableCollection).if {
				sizes.collect { |size, j|
					(size == 0).if { sig[j][0] }{ sig[j] }
				}
			}{
				(sizes == 0).if { sig[0][0] }{ sig.collect(_[0]) }
			}
		};

		// nils in the depth matrix indicate positions to insert zeros.
		// This can save many ugens in the case of multichannel feedback/feedforward
		// with differentiated lookback size.

		var fillWithNils = { |array|
			var maxSize = array.collect(_.size).maxItem;
			array.collect(_.extend(maxSize, nil))
		};

		// blockCount works different here from ar:
		// it counts absolutely, modulo is taken per Buffer

		blockCount = Main.versionAtLeast(3, 9).if {
			Duty.kr(ControlDur.ir, 0, Dseries(0, 1, inf))
		}{
			// correct init bug for older SC version
			Duty.kr(ControlDur.ir, 0, Dseries(0, 1, inf) - 1)
		};

		inSize = in.isKindOf(SequenceableCollection).if { in.collect(_.size) }{ in.size };

		// convention: indices start from 0 and increase with time,
		// thus an init array is stored in reverse order (see checkInits)
		inBufs = makeBufs.(inSize, inDepth);
		outBufs = makeBufs.(outSize, outDepth);

		outType = getOutType.(outSize);

		#inDepth, outDepth = this.checkInits(inBufs, outBufs, inInit, outInit, inDepth, outDepth);

		// depths were passed per signal, but we rather want to iterate over lookback indices

		inDepthFlop = fillWithNils.(inDepth).flop;
		outDepthFlop = fillWithNils.(outDepth).flop;

		// same structure to bring encoded feedback and feedforward signals
		// to desired format for func's 'in' and 'out'

		// differs from ar !
		makeFeedSig = { |depthFlop, ioBufs, sizes, blockCount|
			var sig;
			depthFlop.collect { |depths, j|
				sig = ioBufs.collect { |bufs, k|
					depths[k].isNil.if { [0] }{
						// differs from ar here
						BufRd.kr(1, bufs, (blockCount-depths[k]) % (bufs.collect(_.numFrames)))
					}
				};
				shapeSig.(sig, sizes);
			}
		};


		// begin of core definition
		// 3 versions of implementation

		// with graphOrderType = 1 (default), buffer reading and writing is forced
		// to its desired order with the use of sum and <!

		// with graphOrderType = 2, buffer reading and writing is forced
		// to its desired order with the use of iterated <! operators

		// graphOrderType = 0:
		// as all my test examples worked the same without forced graph ordering,
		// I provide this implementation too as it saves a lot of ugens


		case
		{ (graphOrderType == 1) or: { graphOrderType == 2 } }{

			// default implementation: graphOrderType == 1
			orderSym = (graphOrderType == 1).if { '+' }{ '<!' };

			doThis = DC.kr(0);

			inBufs.collect { |bufs, j|
				bufs.collect { |buf, k|
					doThis = orderSym.applyTo(
						doThis,
						BufWr.kr(indexDispatch.(in, j, k), buf, blockCount % buf.numFrames)
					)
				}
			};

			// here comes actual fb/ff relation into play
			// it can refer to former 'in'/'out' values (see makeFeedSig definition)
			iteratedOut = func.value(
				makeFeedSig.(inDepthFlop, inBufs, inSize, blockCount),
				makeFeedSig.(outDepthFlop, outBufs, outSize, blockCount),
				0
			);

			// the calculated next sample(s) written to buffer(s)
			outBufs.collect { |bufs, j|
				bufs.collect { |buf, k|
					doThis = orderSym.applyTo(
						doThis,
						BufWr.kr(
							outDispatch.(iteratedOut, j, k, outType),
							buf,
							blockCount % buf.numFrames
						)
					)
				}
			};

			// '<! doThis' - want last BufRd to be before BufWrs !
			unshapedOut = outBufs.collect { |bufs, j|
				bufs.collect { |buf, k|
					BufRd.kr(1, buf, (blockCount % buf.numFrames) <! doThis)
				}
			};

			outSig = shapeSig.(unshapedOut, outSize);
		}

		{ graphOrderType == 0 }{
			// alternative implementation without forced order of
			// buffer reading and writing

			inBufs.collect { |bufs, j|
				bufs.collect { |buf, k|
					BufWr.kr(indexDispatch.(in, j, k), buf, blockCount % buf.numFrames)
				}
			};

			// here comes actual fb/ff relation into play
			// it can refer to former 'in'/'out' values (see makeFeedSig definition)
			iteratedOut = func.value(
				makeFeedSig.(inDepthFlop, inBufs, inSize, blockCount),
				makeFeedSig.(outDepthFlop, outBufs, outSize, blockCount),
				0
			);

			// the calculated next sample(s) written to buffer(s)
			outBufs.collect { |bufs, j|
				bufs.collect { |buf, k|
					BufWr.kr(
						outDispatch.(iteratedOut, j, k, outType),
						buf,
						blockCount % buf.numFrames
					)
				}
			};

			unshapedOut = outBufs.collect { |bufs, j|
				bufs.collect { |buf, k|
					BufRd.kr(1, buf, blockCount % buf.numFrames)
				}
			};

			outSig = shapeSig.(unshapedOut, outSize);
		}
		{ true }{ SimpleInitError("wrong graphOrderType, must be 0, 1 or 2, see help").throw };

		^leakDC.if { LeakDC.kr(outSig, leakCoef) }{ outSig }
	}
}



