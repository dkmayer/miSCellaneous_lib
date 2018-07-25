/*
	This file is part of miSCellaneous, a program library for SuperCollider 3

	Created: 2018-07-25, version 0.21
	Copyright (C) 2009-2018 Daniel Mayer
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


AbstractDX : UGen {

	*xr { |type = \mix, outRate = \ar, inOut, channelsArrayRef, fadeTime = 1, stepTime = 1,
			fadeMode = 0, sine = 1, equalPower = 1, power = 1, curve = 0, allowTypeSeq = 0,
			fadeRate = \ar, maxFadeNum = inf, maxWidth = 2, width = 2,
			initOutOffset = 0, maxDynOutOffset = 1, dynOutOffset = 0,
			allowFadeEnd = 1, size = nil, bus = nil, zeroThr = nil, doneAction = 0|

		var maxArgSize, args;

		args = [
			inOut, channelsArrayRef, fadeTime, stepTime, fadeMode, sine, equalPower, power,
			curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, size, bus, zeroThr, doneAction
		];

		#inOut, channelsArrayRef, fadeTime, stepTime, fadeMode, sine, equalPower, power,
			curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, size, bus, zeroThr, doneAction =
				this.unbubbleInputIfNecessary(*args);

		maxArgSize = (args.collect { |x| x.asArray.size }).maxItem;

		this.checkForWrongDrateInput(maxArgSize, inOut, fadeTime, stepTime, sine,
			equalPower, power, curve);

		#inOut, fadeTime, stepTime, sine, equalPower, power, curve =
			[inOut, fadeTime, stepTime, sine, equalPower, power, curve]
			.collect { |x| x.miSC_Dmultiply(maxArgSize) };


		^((type == \fanOut) || (type == \envFanOut)).if {
			this.multiNewList([type, outRate, inOut, channelsArrayRef, fadeTime, stepTime,
				fadeMode, sine, equalPower, power, curve, allowTypeSeq, fadeRate, maxFadeNum,
				maxWidth, width, initOutOffset, maxDynOutOffset, dynOutOffset, allowFadeEnd,
				size, bus, zeroThr, doneAction
			]);
			0.0		// no output
		}{
			this.multiNewList([type, outRate, inOut, channelsArrayRef, fadeTime, stepTime,
				fadeMode, sine, equalPower, power, curve, allowTypeSeq, fadeRate, maxFadeNum,
				maxWidth, width, initOutOffset, maxDynOutOffset, dynOutOffset, allowFadeEnd,
				size, bus, zeroThr, doneAction
			])
		}
	}

	*new1 { |type, outRate, inOut, channelsArrayRef, fadeTime = 1, stepTime = 1, fadeMode = 0,
			sine = 1, equalPower = 1, power = 1, curve = 0, allowTypeSeq = 0,
			fadeRate = \ar, maxFadeNum = inf, maxWidth = 2, width = 2, initOutOffset = 0,
			maxDynOutOffset = 1, dynOutOffset = 0, allowFadeEnd = 1,
			size = nil, bus = nil, zeroThr, doneAction = 0|

		var index, trig, indexDrate, channelsArray, overlapChannelSize, thr = 1e-5, count,
			maxCountNum, panPos, weight, durs, initForwardNum, compOldPanAz, channelStep, done,
			doneDelayTime, weightAtInit, initMuteIndex, dc, dir, lincurve, source, env;

		this.checkForWrongWidthInput(maxWidth, width);
		this.checkForWrongFadeTimeInput(fadeTime, stepTime, fadeMode);

		overlapChannelSize = maxWidth.ceil.asInteger + max(initOutOffset.ceil.asInteger, 0) +
			maxDynOutOffset.ceil.asInteger + 1;

		channelsArray = channelsArrayRef.value;
		channelsArray = this.replaceZeroesWithSilence(channelsArray.asUGenInput(this).asArray);

		// ensure right initForwardNum by tolerance
		// for a given maxWidth the centre has to be at index 0 - see doc

		initForwardNum = ((maxWidth + 1 - thr) / 2).round.asInteger +
			initOutOffset.ceil.asInteger + maxDynOutOffset.ceil.asInteger;
		initMuteIndex = initOutOffset + (width + 1 / 2);

		(fadeMode == 0).if {
			durs = Dstutter(2, fadeTime);
			indexDrate = Dseries() - 1;
		}{
			// double polling needed in these cases, thus Dstutter
			switch(fadeMode,
				1, {
					indexDrate = Dstutter(2, Dseries()) - 1;
					durs = Dstutter(2, Dswitch1([stepTime, fadeTime], Dseq([0, 1], inf)));
				},
				2, {
					indexDrate = Dseq([0, Dstutter(2, Dseries())]);
					durs = Dstutter(2, Dswitch1([stepTime, fadeTime], Dseq([1, 0], inf)));
				},
				3, {
					indexDrate = Dstutter(2, Dseries()) - 1;
					fadeTime = Dstutter(2, fadeTime);
					durs = Dstutter(2, Dswitch1([stepTime - fadeTime, fadeTime], Dseq([0, 1], inf)));
				},
				4, {
					indexDrate = Dseq([0, Dstutter(2, Dseries())]);
					fadeTime = Dstutter(2, fadeTime);
					durs = Dstutter(2, Dswitch1([stepTime - fadeTime, fadeTime], Dseq([1, 0], inf)));
				}
			);
		};

		indexDrate = indexDrate + initOutOffset;
		trig = TDuty.perform(fadeRate, durs);

		count = case
			{ fadeMode == 0 }
				{ PulseCount.perform(fadeRate, trig) }
			{ [2, 4].includes(fadeMode) }
				// count twice
				{ PulseCount.perform(fadeRate, ToggleFF.perform(fadeRate, trig)) }
			{ [1, 3].includes(fadeMode) }
				// count twice and omit first trig
				{ PulseCount.perform(fadeRate, 1 - ToggleFF.perform(fadeRate, trig)) };

		index = count % overlapChannelSize;

		maxCountNum = maxFadeNum;
		[1, 3].includes(fadeMode).if { maxCountNum = maxCountNum + 1 };
		DetectSilence.perform(fadeRate, (1 - (count > maxCountNum)), doneAction: doneAction);

		allowFadeEnd = allowFadeEnd.asBoolean;
		allowFadeEnd.if {
			done = {} ! overlapChannelSize;
			doneDelayTime = Timer.perform(fadeRate, trig)
		};

		// the array of bus indices
		// new indices are polled from drate input 'inOut'

		inOut = { |i|
			var initTrig, fadeTrig, demand;

			// first initForwardNum inOut indices must show up immediately for overlap
			initTrig = ((i < initForwardNum).if { Impulse }{ DC }).perform(fadeRate, 0);
			fadeTrig = (index + initForwardNum - 1 % overlapChannelSize - i).abs < thr;

			demand = Demand.perform(
				fadeRate,
				initTrig + fadeTrig,
				0,
				inOut
			);
			allowFadeEnd.if { done[i] = Done.kr(demand) };
			demand
		} ! overlapChannelSize;

		// strange trigger logic needed here, allowFadeEnd = 0 saves a bit CPU
		allowFadeEnd.if {
			done = TDelay.kr(
				DelayL.kr(done.sum, ControlDur.ir, ControlDur.ir),
				A2K.kr(doneDelayTime)
			);
			done = Trig1.kr(done > thr, inf)
		};

		// compensate PanAz ar bug with panPos and orientation in older SC versions
		compOldPanAz = ((fadeRate == \kr) || (Main.versionAtLeast(3, 9))).not;
		channelStep = compOldPanAz.if { 2 / width }{ 2 } / overlapChannelSize;

		// workhorse 1: angle stepping performed by DemandEnvGen
		panPos = dynOutOffset + DemandEnvGen.perform(fadeRate, indexDrate, durs) * channelStep;

		// workhorse 2: channel distribution performed by PanAz
		weight = PanAz.perform(
			fadeRate,
			overlapChannelSize,
			DC.perform(fadeRate, 1),
			allowFadeEnd.if { Gate.perform(fadeRate, panPos, 0.5 - done) }{ panPos },
			1,
			width,
			// compensate PanAz ar bug with panPos and orientation in older SC versions
			compOldPanAz.if { (1 - width) * 0.5 }{ 0 };
		);

		// with extreme short durations occasional large values have been observed
		// they are catched by Gate
		weight = Gate.perform(fadeRate, weight, (weight >= 0) * (weight <= 1));

		zeroThr.notNil.if { weight = weight * (weight > zeroThr) };

		// lincurve helper Function
		lincurve = { |x| x.lincurve_3_9(0, 1, 0, 1, curve * dir.sign) };

		allowTypeSeq.asBoolean.not.if {
			// static xfade type is most efficient
			// determined by Integers for 'sine' and 'equalPower'

			// among static xfades most efficient for equalPower = sin = 1,
			// that's what is done by PanAz, other cases treated here

			equalPower.asBoolean.not.if {
				dir = HPZ1.perform(fadeRate, weight);
				weight = sine.asBoolean.if {
					lincurve.(weight.squared.pow(power))
				}{
					lincurve.((weight.asin * 2 / pi).pow(power))
				};
			}{
				sine.asBoolean.not.if { weight = (weight.asin * 2 / pi).sqrt }
			}
		}{
			// if xfade types should be sequenced
			// all must run in parallel - this has to be allowed explicitely
			(fadeMode > 0).if {
				sine = Dstutter(2, sine);
				equalPower = Dstutter(2, equalPower);
				power = Dstutter(2, power);
				curve = Dstutter(2, curve);
			};
			sine = Demand.perform(fadeRate, trig, 0, sine);
			equalPower = Demand.perform(fadeRate, trig, 0, equalPower);
			power = Demand.perform(fadeRate, trig, 0, power);
			curve = Demand.perform(fadeRate, trig, 0, curve);
			dir = HPZ1.perform(fadeRate, weight);

			weight = Select.perform(fadeRate, sine * 2 + equalPower, [
				lincurve.((weight.asin * 2 / pi).pow(power)),
				(weight.asin * 2 / pi).sqrt,
				lincurve.(weight.squared.pow(power)),
				weight
			])
		};

		// difference between DX classes
		^case
			{ (type == \mix) || (type == \mixIn) }{
				Mix({ |i|
					var in = (type == \mix).if {
						Select.perform(outRate, inOut[i], channelsArray)
					}{
						In.perform(outRate, inOut[i])
					};
					var muteAtInit = initForwardNum + count >= i;
					weight[i] * in * muteAtInit;
				} ! overlapChannelSize)
			}
			{ (type == \fanOut) || (type == \envFanOut) }{
				source = (type == \fanOut).if { channelsArray }{ 1 };
				{ |i|
					var muteAtInit = initForwardNum + count >= i;
					Out.perform(outRate, inOut[i], weight[i] * source * muteAtInit);
				} ! overlapChannelSize;
			}
			{ (type == \fan) || (type == \envFan) }{
				bus.isNil.if {
					// for larger sizes we have a ugen number growth of quadratic order,
					// thus rather pass a bus then
					dc = { |i| DC.perform(fadeRate, i) ! overlapChannelSize } ! size;
					weightAtInit = { |i| initMuteIndex + count >= i } ! overlapChannelSize;
					weight = weight * weightAtInit;

					env = { |i| Mix(((dc[i] - (inOut.round)).abs < thr) * weight) } ! size;

					(type == \envFan).if {
						env
					}{
						// for multichannel source we need to "convolve" output
						channelsArray = channelsArray.asArray;
						size.collect { |i|
							var sum = 0;
							([i + 1, channelsArray.size, size - i + channelsArray.size - 1].minItem).do { |j|
								sum = sum + (env[i-j] * channelsArray[j])
							};
							sum
						}
					}
				}{
					source = (type == \fan).if { channelsArray }{ 1 };
					{ |i|
						var weightAtInit = initMuteIndex + count >= i;
						Out.perform(
							outRate, bus + inOut[i] + thr,
							weight[i] * weightAtInit * source
						)
					} ! overlapChannelSize;
					In.perform(outRate, bus, size);
				}
			}
	}

	// restrict to possible drate arguments
	*checkForWrongDrateInput { |maxArgSize, inOut, fadeTime, stepTime, sine, equalPower, power, curve|

		if ((maxArgSize > 1) and: {
			[inOut, fadeTime, stepTime, sine, equalPower, power, curve].any { |x|
				x.isKindOf(DUGen) or: {
					x.isKindOf(SequenceableCollection) and: {
						// do deep check, but only for DUGens
						(x.size < maxArgSize) and: { x.flat.any { |y| y.isKindOf(DUGen) } }
					}
				}
			}
		}) {
			SimpleInitError("For multichannel expansion demand rate " ++
				"ugens, that are needed more than once, have to be " ++
				"wrapped into Functions, see help file"
			).throw
		};
	}

	*checkForWrongWidthInput { |maxWidth, width|
		maxWidth.isNumber.not.if {
			SimpleInitError("maxWidth must be a number").throw
		};
		((maxWidth.isNumber && width.isNumber) and: { width > maxWidth }).if {
			SimpleInitError("width must not be larger than maxWidth").throw
		};
		(Main.versionAtLeast(3, 9)).not and: {
			width.isNumber.not.if {
				SimpleInitError("In SC versions before 3.9 width is not modulatable, " ++
					"it must be a number"
				).throw
			}
		};
	}

	*checkForWrongFadeTimeInput { |fadeTime, stepTime, fadeMode|
		((fadeTime.isNumber && stepTime.isNumber && ([3, 4].includes(fadeMode))) and: {
			fadeTime >= stepTime }).if {
			SimpleInitError("fadeTime must not be larger than stepTime").throw
		}
	}

	*unbubbleInputIfNecessary { |... argList|
		^argList.collect { |item|
			((item.isKindOf(SequenceableCollection)) and: { item.size == 1 }).if {
				item[0]
			}{
				item
			};
		}
	}

}


DXMix : AbstractDX {

	*ar { |in, channelsArrayRef, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \ar,
			maxFadeNum = inf, maxWidth = 2, width = 2, initOutOffset = 0, maxDynOutOffset = 1,
			dynOutOffset = 0, allowFadeEnd = 1, zeroThr = nil, doneAction = 0|

		^this.xr(\mix, \ar, in, channelsArrayRef, fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, nil, nil, zeroThr, doneAction
		);
	}

	*kr { |in, channelsArrayRef, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \kr, maxFadeNum = inf, maxWidth = 2,
			width = 2, initOutOffset = 0, maxDynOutOffset = 1, dynOutOffset = 0, allowFadeEnd = 1,
			zeroThr = nil, doneAction = 0|

		^this.xr(\mix, \kr, in, channelsArrayRef, fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, nil, nil, zeroThr, doneAction
		);
	}
}

DXMixIn : AbstractDX {

	*ar { |in, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \ar,
			maxFadeNum = inf, maxWidth = 2, width = 2, initOutOffset = 0, maxDynOutOffset = 1,
			dynOutOffset = 0, allowFadeEnd = 1, zeroThr = nil, doneAction = 0|

		^this.xr(\mixIn, \ar, in, `[], fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, nil, nil, zeroThr, doneAction
		);
	}

	*kr { |in, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \kr, maxFadeNum = inf, maxWidth = 2,
			width = 2, initOutOffset = 0, maxDynOutOffset = 1, dynOutOffset = 0, allowFadeEnd = 1,
			zeroThr = nil, doneAction = 0|

		^this.xr(\mixIn, \kr, in, `[], fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, nil, nil, zeroThr, doneAction
		);
	}
}


DXFan : AbstractDX {

	*ar { |out, channelsArrayRef, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \ar,
			maxFadeNum = inf, maxWidth = 2, width = 2, initOutOffset = 0, maxDynOutOffset = 1,
			dynOutOffset = 0, allowFadeEnd = 1, size = 2, bus = nil, zeroThr = nil, doneAction = 0|

		^this.xr(\fan, \ar, out, channelsArrayRef, fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, size, bus, zeroThr, doneAction
		);
	}

	*kr { |out, channelsArrayRef, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \kr,
			maxFadeNum = inf, maxWidth = 2, width = 2, initOutOffset = 0, maxDynOutOffset = 1,
			dynOutOffset = 0, allowFadeEnd = 1, size = 2, bus = nil, zeroThr = nil, doneAction = 0|

		^this.xr(\fan, \kr, out, channelsArrayRef, fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, size, bus, zeroThr, doneAction
		);
	}
}


DXEnvFan : AbstractDX {

	*ar { |out, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \ar,
			maxFadeNum = inf, maxWidth = 2, width = 2, initOutOffset = 0, maxDynOutOffset = 1,
			dynOutOffset = 0, allowFadeEnd = 1, size = 2, bus = nil, zeroThr = nil, doneAction = 0|

		^this.xr(\envFan, \ar, out, `[], fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset, maxDynOutOffset,
			dynOutOffset, allowFadeEnd, size, bus, zeroThr, doneAction
		);
	}

	*kr { |out, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \kr,
			maxFadeNum = inf, maxWidth = 2, width = 2, initOutOffset = 0, maxDynOutOffset = 1,
			dynOutOffset = 0, allowFadeEnd = 1, size = 2, bus = nil, zeroThr = nil, doneAction = 0|

		^this.xr(\envFan, \kr, out, `[], fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, size, bus, zeroThr, doneAction
		);
	}
}


DXFanOut : AbstractDX {

	*ar { |out, channelsArrayRef, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \ar,
			maxFadeNum = inf, maxWidth = 2, width = 2, initOutOffset = 0, maxDynOutOffset = 1,
			dynOutOffset = 0, allowFadeEnd = 1, zeroThr = nil, doneAction = 0|

		^this.xr(\fanOut, \ar, out, channelsArrayRef, fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, nil, nil, zeroThr, doneAction
		);
	}

	*kr { |out, channelsArrayRef, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \kr,
			maxFadeNum = inf, maxWidth = 2, width = 2, initOutOffset = 0, maxDynOutOffset = 1,
			dynOutOffset = 0, allowFadeEnd = 1, zeroThr = nil, doneAction = 0|

		^this.xr(\fanOut, \kr, out, channelsArrayRef, fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, nil, nil, zeroThr, doneAction
		);
	}
}


DXEnvFanOut : AbstractDX {

	*ar { |out, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \ar,
			maxFadeNum = inf, maxWidth = 2, width = 2, initOutOffset = 0, maxDynOutOffset = 1,
			dynOutOffset = 0, allowFadeEnd = 1, zeroThr = nil, doneAction = 0|

		^this.xr(\envFanOut, \ar, out, `[],  fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, nil, nil, zeroThr, doneAction
		);
	}

	*kr { |out, fadeTime = 1, stepTime = 1, fadeMode = 0, sine = 1, equalPower = 1,
			power = 1, curve = 0, allowTypeSeq = 0, fadeRate = \kr,
			maxFadeNum = inf, maxWidth = 2, width = 2, initOutOffset = 0, maxDynOutOffset = 1,
			dynOutOffset = 0, allowFadeEnd = 1, zeroThr = nil, doneAction = 0|

		^this.xr(\envFanOut, \kr, out, `[],  fadeTime, stepTime, fadeMode, sine, equalPower,
			power, curve, allowTypeSeq, fadeRate, maxFadeNum, maxWidth, width, initOutOffset,
			maxDynOutOffset, dynOutOffset, allowFadeEnd, nil, nil, zeroThr, doneAction
		);
	}
}



+Object {
	miSC_Dmultiply { ^this }
}

+Function {
	miSC_Dmultiply { |n| ^{ this } ! n }
}

+SequenceableCollection {
	miSC_Dmultiply { |n| ^{ |i| this.wrapAt(i) } ! n }
}

