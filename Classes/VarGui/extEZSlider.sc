
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

+EZSlider {

	miSC_colorize { |color, numColorExp = 0.4, fontColor|
		var light = color.miSC_exp(numColorExp), backGroundColor;
		backGroundColor = (\EZSmoothSlider.asClass.notNil and:
			{ this.isMemberOf(EZSmoothSlider) }).if {
				this.sliderView.hiliteColor_(color);
				light
			}{
				color
			};
		this.setColors(stringBackground: light, sliderBackground: backGroundColor,
			numBackground: light, stringColor: fontColor,
			numStringColor: fontColor, numNormalColor: fontColor);
		^this
	}

	// number of indicated digits derived from controlSpec step

	miSC_adaptToControlStep { |precision = 10, stepEqualZeroDiv = 100|
		var x, stepString, range, estimatedControlStep;

		estimatedControlStep =  (controlSpec.step.abs  <  (10 ** precision.neg)).if {
			(controlSpec.clipHi - controlSpec.clipLo) / stepEqualZeroDiv
		}{
			controlSpec.step
		};
		stepString = estimatedControlStep.miSC_decimalStrings(precision);
		this.round = 10 ** stepString.last.size.neg;
		range = controlSpec.constrain(controlSpec.clipHi) -
			controlSpec.constrain(controlSpec.clipLo);

		(range < (10 ** precision.neg)).if {
			// dummy controlspec should look inactive everywhere
			this.sliderView.enabled = false;
			this.numberView.enabled = false;
			this.isMemberOf(EZSlider).if {
				this.sliderView.thumbSize = 0;
			}
		}{
			sliderView.step = 1 / (range / estimatedControlStep).round;
		};
		this.isMemberOf(EZSlider).if {
			numberView.maxDecimals = stepString.last.size
		};
		// take modifiers for multiple slider handling
		sliderView.alt_scale_(1);
		sliderView.ctrl_scale_(1);
		sliderView.shift_scale_(1);
		numberView.alt_scale_(1);
		numberView.ctrl_scale_(1);
		numberView.shift_scale_(1);
		^this
	}

	miSC_mode_ { |type|
		// subclasses EZSmoothSlider, EZRoundSlider may not be defined
		// so distinguish here ...
		^this.isMemberOf(EZSlider).if { this }{ this.sliderView.perform(\mode_, type); this }	}
}
