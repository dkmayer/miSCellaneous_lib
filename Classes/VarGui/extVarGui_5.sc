
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


+SequenceableCollection {
	
	miSC_partitionIndex { |index|
		var i = 0, sum = this.first;
		^(this.size != 0).if {
			while { (index + 1) > sum }{
				i = i + 1;
				(i == this.size).if {
					sum = index + 1;
					i = nil;
				}{
					sum = sum + this[i];
				}
			}; 
			i;
		};
	}
}

+VarGui {
	envirFromSliderIndex { |sliderIndex|
		var varIndex = varCtrSizes.miSC_partitionIndex(sliderIndex), envir;
		^envirs[varEnvirIndices[varIndex]];
	}
	
	addSliderAction { |function,  type, index, envir|
		// index means sliderIndex
		// if type == nil suppose \var
		// if index == nil suppose all indices of that type
		var view, sliders, i;
		type = type ?? \var;
		sliders = (type == \var).if { 
			this.varCtrSliders 
		}{ 
			this.synthCtrSliders 
		};
		(index.isNil.if { sliders }{ sliders[[index]] }).do { |slider, j|
			view = slider.sliderView;
			i = index ?? j;
			envir = envir ?? {
				(type == \var).if {
					this.envirFromSliderIndex(i)
				}{
					currentEnvironment;
				}
			};
			view.mouseUpAction = function.inEnvir(envir);
		};
	}
}
