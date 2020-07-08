
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


Fb1_ODEintdef {

	// container for stepper Functions representing numeric ODE solvers
	// composition of stepper with ODE Function gives the Function to be used within Fb1

	classvar all, initKeys;

	var <name, <function, <stepDepth, <sizeFactor;

	// multi-step procedures have stepDepth > 1
	// not to be confused with single-step procedures with sub-steps (like Runge-Kutta)

	// sizeFactor indicates whether ODE function values are buffered
	// for some procedures this is necessary (ab, abm, pec, pece etc.)
	// for others there exist procedures without and with buffering (e.g. so there is rk3 and rk3_d)

	*new { |name, function, stepDepth = 1, sizeFactor = 1|
		^super.newCopyArgs(name, function, stepDepth, sizeFactor)
			.initFb1_ODEintdef(name);
	}

	*at { |key| ^all.at(key) }

	*keys { |key| ^all.keys.copy }

	*remove { |key| initKeys.includes(key).not.if { all.put(key, nil) } }

	// remove all added Fb1_ODEintdefs
	*reset { all.do { |x| this.remove(x.name) } }

	*postAll {
		var nameStrings = (this.keys.asArray.collect { |x| x.asString }).sort;
		"\nCurrent Fb1_ODEintdefs: \n".postln;
		nameStrings.do { |str| str.postln; };
		"".postln;
	}

	*initClass {
		var symplecticOp;

		all = IdentityDictionary.new;

		// constructors need unified args,
		// thus time and size are always defined and not necessarily needed

		// y is expected to be an array (state vector from the ODE system)
		// for muiltistep procedures it expects
		// an array of arrays (including previous states)

		// the _d variants produce channel(s) for the differential (resp. ODE function values)
		// whenever the main integration doesn't need to employ such anyway

		// It's recommended to work with symplectic integrators
		// as they are better for long-time stability, which is necessary for oscillatory solutions!
		// Also it's easy to get symplectic integration of higher order,
		// simply adapt for order 2k by dividing dt and repeat operation

		symplecticOp = { |order, odeDef, t, y, dt, size, args|
			var newArgs = y.copy, yMid = y.copy, yNew = 0!size, or = 1/order;

			or = 1/order;

			div(order, 2).do { |k|
				(k > 0).if {
					newArgs = yNew.copy;
					yMid = yNew.copy;
				};
				for(0, size-1, { |i|
					(i > 0).if { newArgs[i-1] = yMid[i-1] };
					yMid[i] = odeDef.(i, t, newArgs, *args) * dt * or + yMid[i];
				});
				for(size-1, 0, { |i|
					(i + 1 < size).if { newArgs[i+1] = yNew[i+1] };
					yNew[i] = odeDef.(i, t, newArgs, *args) * dt * or + yMid[i];
				});
			};
			yNew
		};

		Fb1_ODEintdef(\sym2,
			{ |odeDef, t, y, dt, size ... args|
				symplecticOp.(2, odeDef, t, y, dt, size, args);
			}, 1, 1);


		// in the '_d' variants y has doubled size but that doesn't matter as
		// size is passed to symplecticOp
		Fb1_ODEintdef(\sym2_d,
			{ |odeDef, t, y, dt, size ... args|
				var yNew = symplecticOp.(2, odeDef, t, y, dt, size, args);
				var fNew = fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);


		Fb1_ODEintdef(\sym4,
			{ |odeDef, t, y, dt, size ... args|
				symplecticOp.(4, odeDef, t, y, dt, size, args);
			}, 1, 1);


		Fb1_ODEintdef(\sym4_d,
			{ |odeDef, t, y, dt, size ... args|
				var yNew = symplecticOp.(4, odeDef, t, y, dt, size, args);
				var fNew = fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);


		Fb1_ODEintdef(\sym6,
			{ |odeDef, t, y, dt, size ... args|
				symplecticOp.(6, odeDef, t, y, dt, size, args);
			}, 1, 1);


		Fb1_ODEintdef(\sym6_d,
			{ |odeDef, t, y, dt, size ... args|
				var yNew = symplecticOp.(6, odeDef, t, y, dt, size, args);
				var fNew = fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);


		Fb1_ODEintdef(\sym8,
			{ |odeDef, t, y, dt, size ... args|
				symplecticOp.(8, odeDef, t, y, dt, size, args);
			}, 1, 1);


		Fb1_ODEintdef(\sym8_d,
			{ |odeDef, t, y, dt, size ... args|
				var yNew = symplecticOp.(8, odeDef, t, y, dt, size, args);
				var fNew = fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);


		Fb1_ODEintdef(\sym12,
			{ |odeDef, t, y, dt, size ... args|
				symplecticOp.(12, odeDef, t, y, dt, size, args);
			}, 1, 1);


		Fb1_ODEintdef(\sym12_d,
			{ |odeDef, t, y, dt, size ... args|
				var yNew = symplecticOp.(12, odeDef, t, y, dt, size, args);
				var fNew = fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);


		Fb1_ODEintdef(\sym16,
			{ |odeDef, t, y, dt, size ... args|
				symplecticOp.(16, odeDef, t, y, dt, size, args);
			}, 1, 1);


		Fb1_ODEintdef(\sym16_d,
			{ |odeDef, t, y, dt, size ... args|
				var yNew = symplecticOp.(16, odeDef, t, y, dt, size, args);
				var fNew = fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);


		Fb1_ODEintdef(\sym32,
			{ |odeDef, t, y, dt, size ... args|
				symplecticOp.(32, odeDef, t, y, dt, size, args);
			}, 1, 1);


		Fb1_ODEintdef(\sym32_d,
			{ |odeDef, t, y, dt, size ... args|
				var yNew = symplecticOp.(32, odeDef, t, y, dt, size, args);
				var fNew = fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);


		Fb1_ODEintdef(\sym64,
			{ |odeDef, t, y, dt, size ... args|
				symplecticOp.(64, odeDef, t, y, dt, size, args);
			}, 1, 1);


		Fb1_ODEintdef(\sym64_d,
			{ |odeDef, t, y, dt, size ... args|
				var yNew = symplecticOp.(64, odeDef, t, y, dt, size, args);
				var fNew = fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);


		// Euler simple (1-step RK), not recommended !
		Fb1_ODEintdef(\eu,
			{ |odeDef, t, y, dt, size ... args|
				odeDef.(nil, t, y, *args) * dt + y
			}, 1, 1);

		Fb1_ODEintdef(\eu_d,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, yNew, fNew;
				yy = y[0..size-1];
				ff = y[size..2*size-1];
				yNew = ff * dt + yy;
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);

		// Euler modified (2-step RK)
		Fb1_ODEintdef(\eum,
			{ |odeDef, t, y, dt, size ... args|
				var yy = odeDef.value(nil, t, y, *args) * dt * 0.5 + y;
				odeDef.(nil, dt * 0.5 + t, yy, *args) * dt + y;
			}, 1, 1);

		Fb1_ODEintdef(\eum_d,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, z, yNew, fNew;
				yy = y[0..size-1];
				ff = y[size..2*size-1];
				z = ff * dt * 0.5 + yy;
				yNew = odeDef.(nil, dt * 0.5 + t, z, *args) * dt + yy;
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);

		// Euler improved (Heun), 2-step RK
		Fb1_ODEintdef(\eui,
			{ |odeDef, t, y, dt, size ... args|
				var e = odeDef.(nil, t, y, *args);
				var z = e * dt + y;
				(odeDef.(nil, t + dt, z, *args) + e) * dt * 0.5 + y
			}, 1, 1);

		Fb1_ODEintdef(\eui_d,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, z, yNew, fNew;
				yy = y[0..size-1];
				ff = y[size..2*size-1];
				z = ff * dt + yy;
				yNew = (odeDef.(nil, t + dt, z, *args) + ff) * dt * 0.5 + yy;
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);


		// variants of prediction-evaluation-correction
		// using explicit Euler and trapezoidal rule
		Fb1_ODEintdef(\pec,
			{ |odeDef, t, y, dt, size ... args|
				var tn = t + dt;
				var y0 = y[0..size-1];
				var y1 = y[size..2*size-1];
				var p = y1 * dt + y0;
				var pe = odeDef.(nil, tn, p, *args);
				var pec = (y1 + pe) * dt * 0.5 + y0;
				pec ++ pe
			}, 1, 2);

		Fb1_ODEintdef(\pecec,
			{ |odeDef, t, y, dt, size ... args|
				var tn = t + dt;
				var y0 = y[0..size-1];
				var y1 = y[size..2*size-1];
				var p = y1 * dt + y[0..size-1];
				var pe = odeDef.(nil, tn, p, *args);
				var pec = (y1 + pe) * dt * 0.5 + y0;
				var pece = odeDef.(nil, tn, pec, *args);
				var pecec = (y1 + pece) * dt * 0.5 + y0;
				pecec ++ pece
			}, 1, 2);

		Fb1_ODEintdef(\pece,
			{ |odeDef, t, y, dt, size ... args|
				var tn = t + dt;
				var y0 = y[0..size-1];
				var y1 = y[size..2*size-1];
				var p = y1 * dt + y0;
				var pe = odeDef.(nil, tn, p, *args);
				var pec = (y1 + pe) * dt * 0.5 + y0;
				var pece = odeDef.(nil, tn, pec, *args);
				pec ++ pece
			}, 1, 2);

		Fb1_ODEintdef(\pecece,
			{ |odeDef, t, y, dt, size ... args|
				var tn = t + dt;
				var y0 = y[0..size-1];
				var y1 = y[size..2*size-1];
				var p = y1 * dt + y0;
				var pe = odeDef.(nil, tn, p, *args);
				var pec = (y1 + pe) * dt * 0.5 + y0;
				var pece = odeDef.(nil, tn, pec, *args);
				var pecec = (y1 + pece) * dt * 0.5 + y0;
				var pecece = odeDef.(nil, tn, pecec, *args);
				pecec ++ pecece
			}, 1, 2);


		// Runge-Kutta 3-step
		Fb1_ODEintdef(\rk3,
			{ |odeDef, t, y, dt, size ... args|
				var k1 = odeDef.(nil, t, y, *args);
				var k2 = odeDef.(nil, dt * 0.5 + t, k1 * dt * 0.5 + y, *args);
				var k3 = odeDef.(nil, dt + t, dt * 2 * k2 - (dt * k1) + y, *args);
				(k1 + (4 * k2) + k3) * dt / 6 + y
			}, 1, 1);

		Fb1_ODEintdef(\rk3_d,
			{ |odeDef, t, y, dt, size ... args|
				var k1, k2, k3, yy, ff, yNew, fNew;
				yy = y[0..size-1];
				ff = y[size..2*size-1];
				k1 = ff;
				k2 = odeDef.(nil, dt * 0.5 + t, k1 * dt * 0.5 + yy, *args);
				k3 = odeDef.(nil, dt + t, dt * 2 * k2 - (dt * k1) + yy, *args);
				yNew = (k1 + (4 * k2) + k3) * dt / 6 + yy;
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);


		// Runge-Kutta 3-step (Heun)
		Fb1_ODEintdef(\rk3h,
			{ |odeDef, t, y, dt, size ... args|
				var k1 = odeDef.(nil, t, y, *args);
				var k2 = odeDef.(nil, dt / 3 + t, k1 * dt / 3 + y, *args);
				var k3 = odeDef.(nil, dt * 2/3 + t, dt * (2/3) * k2 + y, *args);
				(3 * k3 + k1) * dt * 0.25 + y
			}, 1, 1);

		Fb1_ODEintdef(\rk3h_d,
			{ |odeDef, t, y, dt, size ... args|
				var k1, k2, k3, yy, ff, yNew, fNew;
				yy = y[0..size-1];
				ff = y[size..2*size-1];
				k1 = ff;
				k2 = odeDef.(nil, dt / 3 + t, k1 * dt / 3 + yy, *args);
				k3 = odeDef.(nil, dt * 2/3 + t, dt * (2/3) * k2 + yy, *args);
				yNew = (3 * k3 + k1) * dt * 0.25 + yy;
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);

		// Runge-Kutta 4-step (classical)
		Fb1_ODEintdef(\rk4,
			{ |odeDef, t, y, dt, size ... args|
				var k1 = odeDef.(nil, t, y, *args);
				var k2 = odeDef.(nil, dt * 0.5 + t, k1 * dt * 0.5 + y, *args);
				var k3 = odeDef.(nil, dt * 0.5 + t, k2 * dt * 0.5 + y, *args);
				var k4 = odeDef.(nil, dt + t, k3 * dt + y, *args);
				((k2 + k3) * 2 + k1 + k4) * dt / 6 + y
			}, 1, 1);

		Fb1_ODEintdef(\rk4_d,
			{ |odeDef, t, y, dt, size ... args|
				var k1, k2, k3, k4, yy, ff, yNew, fNew;
				yy = y[0..size-1];
				ff = y[size..2*size-1];
				k1 = ff;
				k2 = odeDef.(nil, dt * 0.5 + t, k1 * dt * 0.5 + yy, *args);
				k3 = odeDef.(nil, dt * 0.5 + t, k2 * dt * 0.5 + yy, *args);
				k4 = odeDef.(nil, dt + t, k3 * dt + yy, *args);
				yNew = ((k2 + k3) * 2 + k1 + k4) * dt / 6 + yy;
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 1, 2);


		// Adams-Bashforth multi-step
		// ab Functions are not optimized for iterated language use
		// doesn't matter though if merged into the UGenGraphFunction

		// y expects array of arrays !
		// y[0] contains previous y and and previous function values

		Fb1_ODEintdef(\ab2,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, yNew, fNew;
				yy = y.collect(_[0..size-1]); // prev y
				ff = y.collect(_[size..2*size-1]); // prev function values
				yNew = (3 * ff[0] - ff[1]) * dt * 0.5 + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 2, 2);

		Fb1_ODEintdef(\ab3,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				yNew = (23/12 * ff[0] - (16/12 * ff[1]) + (5/12 * ff[2])) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 3, 2);

		Fb1_ODEintdef(\ab4,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				yNew = (55/24 * ff[0] - (59/24 * ff[1]) + (37/24 * ff[2]) -
					(9/24 * ff[3])) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 4, 2);

		Fb1_ODEintdef(\ab5,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				yNew = (1901/720 * ff[0] - (2774/720 * ff[1]) + (2616/720 * ff[2]) -
					(1274/720 * ff[3]) + (251/720 * ff[4])) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 5, 2);

		Fb1_ODEintdef(\ab6,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				yNew = (4277/1440 * ff[0] - (7923/1440 * ff[1]) + (9982/1440 * ff[2]) -
					(7298/1440 * ff[3]) + (2877/1440 * ff[4]) - (475/1440 * ff[5])) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 6, 2);

		// Adams-Bashforth-Moulton predictor corrector
		// theory suggests to combine k-step predictor with (k-1) step corrector
		// y expects array of arrays !
		// y[0] contains previous y and and previous function values

		Fb1_ODEintdef(\abm21,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, p, fp, yNew, fNew;
				yy = y.collect(_[0..size-1]); // prev y
				ff = y.collect(_[size..2*size-1]); // prev function values
				p = (3 * ff[0] - ff[1]) * dt * 0.5 + yy[0]; // Adams-Bashforth predictor
				fp = odeDef.(nil, t + dt, p, *args);
				yNew = (fp + ff[0]) * dt * 0.5 + yy[0]; // Adams-Moulton corrector
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 2, 2);

		Fb1_ODEintdef(\abm22,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, p, fp, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				p = (3 * ff[0] - ff[1]) * dt * 0.5 + yy[0];
				fp = odeDef.(nil, t + dt, p, *args);
				yNew = ((5/12 * fp) + (8/12 * ff[0]) - (ff[1]/12)) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 2, 2);

		Fb1_ODEintdef(\abm32,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, p, fp, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				p = (23/12 * ff[0] - (4/3 * ff[1]) + (5/12 * ff[2])) * dt + yy[0];
				fp = odeDef.(nil, t + dt, p, *args);
				yNew = ((5/12 * fp) + (2/3 * ff[0]) - (ff[1]/12)) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 3, 2);

		Fb1_ODEintdef(\abm33,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, p, fp, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				p = (23/12 * ff[0] - (4/3 * ff[1]) + (5/12 * ff[2])) * dt + yy[0];
				fp = odeDef.(nil, t + dt, p, *args);
				yNew = ((3/8 * fp) + (19/24 * ff[0]) - (5/24 * ff[1]) + (ff[2]/24)) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 3, 2);

		Fb1_ODEintdef(\abm43,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, p, fp, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				p = (55/24 * ff[0] - (59/24 * ff[1]) + (37/24 * ff[2]) -
					(3/8 * ff[3])) * dt + yy[0];
				fp = odeDef.(nil, t + dt, p, *args);
				yNew = ((3/8 * fp) + (19/24 * ff[0]) - (5/24 * ff[1]) + (ff[2]/24)) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 4, 2);

		Fb1_ODEintdef(\abm44,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, p, fp, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				p = (55/24 * ff[0] - (59/24 * ff[1]) + (37/24 * ff[2]) -
					(3/8 * ff[3])) * dt + yy[0];
				fp = odeDef.(nil, t + dt, p, *args);
				yNew = ((251/720 * fp) + (646/720 * ff[0]) - (264/720 * ff[1]) +
					(106/720 * ff[2]) - (19/720 * ff[3])) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 4, 2);

		Fb1_ODEintdef(\abm54,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, p, fp, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				p = (1901/720 * ff[0] - (2774/720 * ff[1]) + (2616/720 * ff[2]) -
					(1274/720 * ff[3]) + (251/720 * ff[4])) * dt + yy[0];
				fp = odeDef.(nil, t + dt, p, *args);
				yNew = ((251/720 * fp) + (646/720 * ff[0]) - (264/720 * ff[1]) +
					(106/720 * ff[2]) - (19/720 * ff[3])) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 5, 2);

		Fb1_ODEintdef(\abm55,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, p, fp, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				p = (1901/720 * ff[0] - (2774/720 * ff[1]) + (2616/720 * ff[2]) -
					(1274/720 * ff[3]) + (251/720 * ff[4])) * dt + yy[0];
				fp = odeDef.(nil, t + dt, p, *args);
				yNew = ((475/1440 * fp) + (1427/1440 * ff[0]) - (798/1440 * ff[1]) +
					(482/1440 * ff[2]) - (173/1440 * ff[3]) + (27/1440 * ff[4])) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 5, 2);

		Fb1_ODEintdef(\abm65,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, p, fp, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				p = (4277/1440 * ff[0] - (7923/1440 * ff[1]) + (9982/1440 * ff[2]) -
					(7298/1440 * ff[3]) + (2877/1440 * ff[4]) - (475/1440 * ff[5])) * dt + yy[0];
				fp = odeDef.(nil, t + dt, p, *args);
				yNew = ((475/1440 * fp) + (1427/1440 * ff[0]) - (798/1440 * ff[1]) +
					(482/1440 * ff[2]) - (173/1440 * ff[3]) + (27/1440 * ff[4])) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 6, 2);

		Fb1_ODEintdef(\abm66,
			{ |odeDef, t, y, dt, size ... args|
				var yy, ff, p, fp, yNew, fNew;
				yy = y.collect(_[0..size-1]);
				ff = y.collect(_[size..2*size-1]);
				p = (4277/1440 * ff[0] - (7923/1440 * ff[1]) + (9982/1440 * ff[2]) -
					(7298/1440 * ff[3]) + (2877/1440 * ff[4]) - (475/1440 * ff[5])) * dt + yy[0];
				fp = odeDef.(nil, t + dt, p, *args);
				yNew = ((19087/60480 * fp) + (65112/60480 * ff[0]) - (46461/60480 * ff[1]) +
					(37504/60480 * ff[2]) - (20211/60480 * ff[3]) + (6312/60480 * ff[4]) -
					(863/60480 * ff[5])) * dt + yy[0];
				fNew = odeDef.(nil, t + dt, yNew, *args);
				yNew ++ fNew
			}, 6, 2);

		initKeys = all.keys;
	}

	initFb1_ODEintdef { |name|

		(initKeys.notNil and: { initKeys.includes(name) }).if {
			var err = SimpleInitError("Fb1_ODEintdef of this name already exists");
			err.class_("Fb1_ODEintdef");
			err.throw;
		};

		all.put(name, this)
	}
}
