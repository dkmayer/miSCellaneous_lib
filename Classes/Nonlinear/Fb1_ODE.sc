
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


Fb1_ODE : UGen {

	*ar { |name, argList, tMul = 1, t0, y0, intType = \sym2,
			compose, composeArIn, dt0, argList0, init_intType = \sym8,
			withOutScale = true, withDiffChannels = false, withTimeChannel = false,
			blockSize, graphOrderType = 1, leakDC = true, leakCoef = 0.995|

		^Fb1_ODE.basicNew(\audio, name, argList, tMul, t0, y0, intType,
			compose, composeArIn, dt0, argList0, init_intType,
			withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*kr { |name, argList, tMul = 1, t0, y0, intType = \sym4,
			compose, dt0, argList0, init_intType = \sym8,
			withOutScale = true, withDiffChannels = false, withTimeChannel = false,
			graphOrderType = 1, leakDC = true, leakCoef = 0.995|

		^Fb1_ODE.basicNew(\control, name, argList, tMul, t0, y0, intType,
			compose, nil, dt0, argList0, init_intType,
			withOutScale, withDiffChannels, withTimeChannel,
			nil, graphOrderType, leakDC, leakCoef)
	}

	// for compliance with Fb1 also for Fb1_ODE: new = ar

	*new { |name, argList, tMul = 1, t0, y0, intType = \sym2,
			compose, composeArIn, dt0, argList0, init_intType = \sym8,
			withOutScale = true, withDiffChannels = false, withTimeChannel = false,
			blockSize, graphOrderType = 1, leakDC = true, leakCoef = 0.995|

		^Fb1_ODE.basicNew(\audio, name, argList, tMul, t0, y0, intType,
			compose, composeArIn, dt0, argList0, init_intType,
			withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}


	*basicNew { |rate, name, argList, tMul = 1, t0, y0, intType = \sym2,
		compose, composeArIn, dt0, argList0, init_intType = \sym8,
		withOutScale = true, withDiffChannels = false, withTimeChannel = false,
		blockSize, graphOrderType = 1, leakDC = true, leakCoef = 0.995|

		var dt, size, stepDepth, sizeFactor, rawIntSize, outSize, outDepth, sBlockSize,
			basicDelta, blockFactor, outInit, sig, composeIns, argIns, argListFlatAr,
			odeDef, odeIntDef, err, tMulIn, tSum, tSumInOld, fb1_in, fb1_func,
			outScale, fb1_intFunc, fb1_inDepth, fb1_inInit, op;
		var templateString, rateString, s, dtRate;

		odeDef = Fb1_ODEdef.at(name);
		odeIntDef = Fb1_ODEintdef.at(intType);

		odeDef.isNil.if {
			err = SimpleInitError("no Fb1_ODEdef of that name").class_("Fb1_ODE").throw;
		};

		odeIntDef.isNil.if {
			err = SimpleInitError("no Fb1_ODEintdef of that name").class_("Fb1_ODE").throw;
		};

		t0 = t0 ?? { odeDef.t0 };
		y0 = (y0 ?? { odeDef.y0 }).asArray;

		[intType, init_intType].do { |sym, i|
			var argSymbols = ["intType", "init_intType"];
			var keys = Fb1_ODEintdef.keys;
			keys.includes(sym).not.if {
				err = SimpleInitError(
					argSymbols[i] ++
					" must be contained in " ++
					keys.postcs
				);
				err.class_("Fb1_ODE");
				err.throw;
			}
		};

		size = odeDef.size;
		stepDepth = odeIntDef.stepDepth;
		sizeFactor = odeIntDef.sizeFactor;
		rawIntSize = sizeFactor * size;

		// rawIntSize: size of arrays used in the iteration process (without accumulated time)
		// this is either the size of the ODE system or its product with a factor 2,
		// then the ODE Function values (= the derivation) are stored for each sample

		// the x_d intTypes exist for cases where we want these values, but
		// where they are not buffered by default (e.g. 'rk3', 'eum', etc.)

		outSize = rawIntSize + 1; // time is stored too !
		outDepth = stepDepth + 1; // for Fb1 the depth value must be lookback + 1 !

		withDiffChannels.if {
			(sizeFactor <= 1).if {
				err = SimpleInitError("withDiffChannels == true only makes sense " ++
					"for an intType Function which produces the differential values, " ++
					"such as \\sym2_d, \\sym4_d, \\sym6_d, \\sym8_d, \\sym12_d, " ++
					"\\sym16_d, \\sym32_d, \\sym64_d, \\eum_d, " ++
					"\\eui_d, \\rk3_d, \\rk3h_d, \\rk4_d, " ++
					"\\pec, \\pece, \\pecec, \\pecece " ++
					"and the \\ab and \\abm types"
				).class_("Fb1_ODE").throw;
			}
		};

		argList = argList.asArray;
		argList0 = argList0 ?? { argList };

		((sizeFactor > 1) and: { argList0.flat.any(_.isNumber.not) }).if {
			err = SimpleInitError("initial ODE Function values have to be " ++
				"calculated in language and there are non-number items (UGens) " ++
				"contained in argList. Either take an intType where initial " ++
				"calculation is not necessary (e.g. \\sym2, \\sym4, etc.) or pass an "
				"additional argList0 arg with numbers only").class_("Fb1_ODE").throw;
		};

		(stepDepth > 1).if {
			templateString =
				"A multi-step procedure is selected, so initial ODE solution " ++
				"approximations have to be calculated in language, ";
			rateString = (rate == \audio).if { "sampleRate" }{ "controlRate" };

			// for multi-step procedures we need the first values (resp. arrays of values)
			dt0 = dt0 ?? {
				s = Server.default;
				dtRate = s.notNil.if {
					(rate == \audio).if {
						s.sampleRate
					}{
						s.sampleRate / s.options.blockSize
					}
				};

				dtRate.isNil.if {
					err = SimpleInitError(templateString ++
						"but no default server is found, so " ++
						"an assumption has to be made for first delta t, " ++
						"please define dt0 in secs, recommended: " ++
						"tMul at start / planned " ++ rateString).class_("Fb1_ODE").throw;
				}{
					tMul.isNumber.not.if {
						err = SimpleInitError(templateString ++
							"but there are non-number items (UGens) contained in tMul, " ++
							"please define dt0 in secs, recommended: " ++
							"tMul at start / planned " ++ rateString).class_("Fb1_ODE").throw;
					}{
						(templateString ++
							"but dt0 is not defined, " ++
							"tMul divided by default server's " ++ rateString ++
								" is taken instead."
						).postln;
						tMul / dtRate
					}
				}
			};
		}{
			// also for single-step procedures we must evaluate the ODE Function for x_d intTypes
			// but for stepDepth == 1 dt0 is not used, no problem if it's a UGen or nil
		};


		outInit = odeDef.makeOutInit(intType, init_intType, t0, y0, dt0, argList0, true);

		(rate == \audio).if {
			op = \ar;

			// we have to build the Fb1 function in a way that it reads ar args.
			// convention: composeArIn first (indices possibly defined in compose Function),
			// then detected ar args from argList, then the integrated time (Sweep) and tMul (if ar)

			// This "external" time integration is absolutely necessary as
			// a binary operator summing of dts leads to drifts !!

			composeArIn = composeArIn.asArray;
			tSum = t0 + Sweep.ar(0, tMul);

			argListFlatAr = argList.flatten.select(_.miSC_isAr);

			fb1_in = composeArIn ++ argListFlatAr ++ tSum ++ (tMul.miSC_isAr.if { tMul });

			fb1_inDepth = (1 ! composeArIn.size) ++
				(1 ! argListFlatAr.size) ++ 2 ++ (tMul.miSC_isAr.if { 1 });

			fb1_inInit = (nil ! composeArIn.size) ++
				(nil ! argListFlatAr.size) ++ t0 ++ (tMul.miSC_isAr.if { [nil] });

			// used in the Fb1 Function
			basicDelta = SampleDur.ir;

			// in this Function integration method and ODE Function are composed
			// it will get passed the out arg from the Fb1 Function
			fb1_intFunc = odeDef.dispatchIntFunc(intType);


			// core: everything packed into the Fb1 function, which contructs the graph iteratively
			fb1_func = { |in, out|
				var lastDt, count = composeArIn.size - 1;
				composeIns = composeArIn.size.collect { |i| in[0][i] };

				#argIns, count = argList.miSC_adaptFb1ArgList(in[0], count);

				tSumInOld = in[1][count + 1];
				tMulIn = tMul.miSC_isAr.if { in[0][count + 2] }{ tMul };

				fb1_intFunc.(out, tSumInOld, basicDelta * tMulIn, argIns)
					.miSC_fb1_perform(compose, composeIns, size, outSize);
			};

			// wrong blockSize can lead to confusion, better check
			sBlockSize = Server.default.notNil.if {
				Server.default.options.blockSize
			};

			blockSize.isNil.if {
				sBlockSize.isNil.if {
					err = SimpleInitError(
						"no default blockSize detected and no blockSize passed")
						.class_("Fb1_ODE").throw;
				}{
					"".postln;
					("Fb1_ODE: taking default server's blockSize " ++ sBlockSize.asString).postln;
					"".postln;
					blockSize = sBlockSize;
				}
			}{
				(blockSize.isInteger and: { blockSize.isPowerOfTwo }).if {
					(sBlockSize != blockSize).if {
						sBlockSize.notNil.if {
							"".postln;
							("blockSize " ++ blockSize.asString ++ "passed to Fb1_ODE " ++
								"unequal to default Server's blockSize " ++
								sBlockSize.asString ++ " - intended ?").warn;
							"".postln;
						}{
							"".postln;
							("Fb1_ODE: no default blockSize detected, " ++
								"taking passed blockSize " ++ blockSize.asString).warn;
							"".postln;
						}
					}
				}{
					err = SimpleInitError("blockSize must be integer power of 2")
						.class_("Fb1_ODE").throw;
				}
			};

			// blockFactor has to be chosen correctly,
			// especially relevant for small blockSizes:

			// e.g. for blockSize 4 a blockFactor of 1 is sufficient
			// only for stepDepth 1-4;
			// for blockSize 1 we need a blockFactor equal to stepDepth,
			// in general:

			blockFactor = (stepDepth - 1).div(blockSize) + 1;

			sig = Fb1.ar(fb1_func, fb1_in,
				outSize: outSize,
				inDepth: fb1_inDepth,
				outDepth: outDepth,
				inInit: fb1_inInit,
				outInit: outInit,
				blockSize: blockSize,
				blockFactor: blockFactor,
				graphOrderType: graphOrderType,
				leakDC: leakDC,
				leakCoef: leakCoef
			);

		}{
			op = \kr;

			// things are more straight here
			// still "external" time integration is better !
			tSum = t0 + Sweep.kr(0, tMul);

			// used in the Fb1 Function
			basicDelta = ControlDur.ir;

			// in this Function integration method and ODE Function are composed
			// it will get passed the out arg from the Fb1 Function
			fb1_intFunc = odeDef.dispatchIntFunc(intType);

			// core: everything packed into the Fb1 function, which contructs the graph iteratively
			fb1_func = { |in, out|
				fb1_intFunc.(out, in[1] /* last time */, basicDelta * tMul, argList)
					.miSC_fb1_perform(compose, [], size, outSize);
			};

			// pass integrated time via 'in' as we need last one
			sig = Fb1.kr(fb1_func, tSum,
				outSize: outSize,
				inDepth: 2,
				outDepth: outDepth,
				inInit: t0,
				outInit: outInit,
				graphOrderType: graphOrderType,
				leakDC: leakDC,
				leakCoef: leakCoef
			)
		};

		// optional default scaling (makes sense for systems like Lorenz)

		outScale = withOutScale.if {
			[odeDef.outScale, odeDef.diffOutScale]
		}{
			[1, 1]
		};

		// options for derivation and time return

		sig = (sig[..size-1] * outScale[0]) ++
			(withDiffChannels.if {
				sig[rawIntSize-size..rawIntSize-1] * outScale[1]
			}{ [] }) ++
			(withTimeChannel.if { tSum }{ [] });

		sig = (outSize == 1).if { sig[0] }{ sig };

		// might be necessary
		^sig <! DC.perform(op, 0)
	}

}


// wrappers for predefined ODEs

Fb1_MSD : UGen {

	*new { |f = 0, mass = 1, spring = 1, dampen = 0, tMul = 1, t0 = 0, y0 = #[0, 0],
		intType = \sym2, compose, composeArIn, dt0, argList0, init_intType = \sym8,
		withDiffChannels = false, withTimeChannel = false, blockSize,
		graphOrderType = 1, leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\MSD, [f, mass, spring, dampen], tMul, t0, y0, intType,
			compose, composeArIn, dt0, argList0, init_intType, false, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*ar { |f = 0, mass = 1, spring = 1, dampen = 0, tMul = 1, t0 = 0, y0 = #[0, 0],
		intType = \sym2, compose, composeArIn, dt0, argList0, init_intType = \sym8,
		withDiffChannels = false, withTimeChannel = false, blockSize,
		graphOrderType = 1, leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\MSD, [f, mass, spring, dampen], tMul, t0, y0, intType,
			compose, composeArIn, dt0, argList0, init_intType, false, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*kr { |f = 0, mass = 1, spring = 1, dampen = 0, tMul = 1, t0 = 0, y0 = #[0, 0],
		intType = \sym4, compose, dt0, argList0, init_intType = \sym8,
		withDiffChannels = false, withTimeChannel = false,
		graphOrderType = 1, leakDC = true, leakCoef = 0.995|

		^Fb1_ODE.kr(\MSD, [f, mass, spring, dampen], tMul, t0, y0, intType,
			compose, dt0, argList0, init_intType, false, withDiffChannels, withTimeChannel,
			graphOrderType, leakDC, leakCoef)
	}
}


Fb1_SD : UGen {

	*new { |f = 0, spring = 1, dampen = 0, tMul = 1, t0 = 0, y0 = #[0, 0], intType = \sym2,
		compose, composeArIn, dt0, argList0, init_intType = \sym8,
		withDiffChannels = false, withTimeChannel = false, blockSize,
		graphOrderType = 1, leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\SD, [f, spring, dampen], tMul, t0, y0, intType,
			compose, composeArIn, dt0, argList0, init_intType, false, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*ar { |f = 0, spring = 1, dampen = 0, tMul = 1, t0 = 0, y0 = #[0, 0], intType = \sym2,
		compose, composeArIn, dt0, argList0, init_intType = \sym8,
		withDiffChannels = false, withTimeChannel = false, blockSize,
		graphOrderType = 1, leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\SD, [f, spring, dampen], tMul, t0, y0, intType,
			compose, composeArIn, dt0, argList0, init_intType, false, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*kr { |f = 0, spring = 1, dampen = 0, tMul = 1, t0 = 0, y0 = #[0, 0], intType = \sym4,
		compose, dt0, argList0, init_intType = \sym8,
		withDiffChannels = false, withTimeChannel = false,
		graphOrderType = 1, leakDC = true, leakCoef = 0.995|

		^Fb1_ODE.kr(\SD, [f, spring, dampen], tMul, t0, y0, intType,
			compose, dt0, argList0, init_intType, false, withDiffChannels, withTimeChannel,
			graphOrderType, leakDC, leakCoef)
	}
}


Fb1_Lorenz : UGen {

	*new { |s = 10, r = 30, b = 2, tMul = 1, t0 = 0, y0 = #[1, 1, 1],
		intType = \sym2, compose, composeArIn, dt0, argList0,
		init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\Lorenz, [s, r, b], tMul, t0, y0, intType, compose, composeArIn, dt0, argList0,
			init_intType, withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*ar { |s = 10, r = 30, b = 2, tMul = 1, t0 = 0, y0 = #[1, 1, 1],
		intType = \sym2, compose, composeArIn, dt0, argList0,
		init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\Lorenz, [s, r, b], tMul, t0, y0, intType, compose, composeArIn, dt0, argList0,
			init_intType, withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*kr { |s = 10, r = 30, b = 2, tMul = 1, t0 = 0, y0 = #[1, 1, 1],
		intType = \sym4, compose, dt0, argList0,
		init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE.kr(\Lorenz, [s, r, b], tMul, t0, y0, intType, compose, dt0, argList0,
			init_intType, withOutScale, withDiffChannels, withTimeChannel,
			graphOrderType, leakDC, leakCoef)
	}
}


Fb1_Hopf : UGen {

	*new { |f = 0, mu = 1, theta = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1], intType = \sym2,
		compose, composeArIn, dt0, argList0, init_intType = \sym8,
		withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\Hopf, [f, mu, theta], tMul, t0, y0, intType,
			compose, composeArIn, dt0, argList0, init_intType,
			withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*ar { |f = 0, mu = 1, theta = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1], intType = \sym2,
		compose, composeArIn, dt0, argList0, init_intType = \sym8,
		withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\Hopf, [f, mu, theta], tMul, t0, y0, intType,
			compose, composeArIn, dt0, argList0, init_intType,
			withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*kr { |f = 0, mu = 1, theta = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1], intType = \sym4,
		compose, dt0, argList0, init_intType = \sym8,
		withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE.kr(\Hopf, [f, mu, theta], tMul, t0, y0, intType,
			compose, dt0, argList0, init_intType,
			withOutScale, withDiffChannels, withTimeChannel,
			graphOrderType, leakDC, leakCoef)
	}
}


Fb1_HopfA : UGen {

	*new { |f = 0, mu = 1, eta = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1, 1], intType = \sym2, compose, composeArIn, dt0, argList0,
		init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\HopfA, [f, mu, eta], tMul, t0, y0, intType, compose, composeArIn, dt0, argList0,
			init_intType, withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*ar { |f = 0, mu = 1, eta = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1, 1], intType = \sym2, compose, composeArIn, dt0, argList0,
		init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\HopfA, [f, mu, eta], tMul, t0, y0, intType, compose, composeArIn, dt0, argList0,
			init_intType, withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*kr { |f = 0, mu = 1, eta = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1, 1], intType = \sym4, compose, dt0, argList0,
		init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE.kr(\HopfA, [f, mu, eta], tMul, t0, y0, intType, compose, dt0, argList0,
			init_intType, withOutScale, withDiffChannels, withTimeChannel,
			graphOrderType, leakDC, leakCoef)
	}
}


Fb1_HopfAFDC : UGen {

	*new { |f = 0, mu = 1, eta = 1, tau = 1, kappa = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1, 0.01, 0.01, 1], intType = \sym2, compose, composeArIn, dt0, argList0,
		init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\HopfAFDC, [f, mu, eta, tau, kappa, y0[2], y0[3]], tMul, t0, y0, intType, compose, composeArIn,
			dt0, argList0, init_intType, withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*ar { |f = 0, mu = 1, eta = 1, tau = 1, kappa = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1, 0.01, 0.01, 1], intType = \sym2, compose, composeArIn, dt0, argList0,
		init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\HopfAFDC, [f, mu, eta, tau, kappa, y0[2], y0[3]], tMul, t0, y0, intType, compose, composeArIn,
			dt0, argList0, init_intType, withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*kr { |f = 0, mu = 1, eta = 1, tau = 1, kappa = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1, 0.01, 0.01, 1], intType = \sym4, compose, dt0, argList0,
		init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE.kr(\HopfAFDC, [f, mu, eta, tau, kappa, y0[2], y0[3]], tMul, t0, y0, intType, compose,
			dt0, argList0, init_intType, withOutScale, withDiffChannels, withTimeChannel,
			graphOrderType, leakDC, leakCoef)
	}
}


Fb1_VanDerPol : UGen {

	*new { |f = 0, mu = 1, theta = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1], intType = \sym2, compose, composeArIn,
		dt0, argList0, init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\VanDerPol, [f, mu, theta], tMul, t0, y0, intType, compose, composeArIn, dt0, argList0,
			init_intType, withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*ar { |f = 0, mu = 1, theta = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1], intType = \sym2, compose, composeArIn,
		dt0, argList0, init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\VanDerPol, [f, mu, theta], tMul, t0, y0, intType, compose, composeArIn, dt0, argList0,
			init_intType, withOutScale, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*kr { |f = 0, mu = 1, theta = 1,
		tMul = 1, t0 = 0, y0 = #[1, 1], intType = \sym4, compose,
		dt0, argList0, init_intType = \sym8, withOutScale = true, withDiffChannels = false,
		withTimeChannel = false, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE.kr(\VanDerPol, [f, mu, theta], tMul, t0, y0, intType, compose, dt0, argList0,
			init_intType, withOutScale, withDiffChannels, withTimeChannel,
			graphOrderType, leakDC, leakCoef)
	}
}


Fb1_Duffing : UGen {

	*new { |f = 0, alpha = 0, beta = 1, gamma = 1, delta = 1, omega = 1,
		tMul = 1, t0 = 0, y0 = #[1, 0], intType = \sym2, compose, composeArIn, dt0, argList0,
		init_intType = \sym8, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\Duffing, [f, alpha, beta, gamma, delta, omega], tMul, t0, y0, intType,
			compose, composeArIn, dt0, argList0, init_intType, true, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*ar { |f = 0, alpha = 0, beta = 1, gamma = 1, delta = 1, omega = 1,
		tMul = 1, t0 = 0, y0 = #[1, 0], intType = \sym2, compose, composeArIn, dt0, argList0,
		init_intType = \sym8, withDiffChannels = false,
		withTimeChannel = false, blockSize, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE(\Duffing, [f, alpha, beta, gamma, delta, omega], tMul, t0, y0, intType,
			compose, composeArIn, dt0, argList0, init_intType, true, withDiffChannels, withTimeChannel,
			blockSize, graphOrderType, leakDC, leakCoef)
	}

	*kr { |f = 0, alpha = 0, beta = 1, gamma = 1, delta = 1, omega = 1,
		tMul = 1, t0 = 0, y0 = #[1, 0], intType = \sym4, compose, dt0, argList0,
		init_intType = \sym8, withDiffChannels = false,
		withTimeChannel = false, graphOrderType = 1,
		leakDC = true, leakCoef = 0.995|

		^Fb1_ODE.kr(\Duffing, [f, alpha, beta, gamma, delta, omega], tMul, t0, y0, intType,
			compose, dt0, argList0, init_intType, true, withDiffChannels, withTimeChannel,
			graphOrderType, leakDC, leakCoef)
	}
}


