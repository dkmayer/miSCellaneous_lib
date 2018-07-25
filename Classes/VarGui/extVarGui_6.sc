
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

+ VarGui {

	midiBindVarSlider { |key, varNum, indexOffset, varSliderIndex, ccNum, chan, srcID|
		^MIDIFunc.cc({ |...args|
			var val, num, chan, src, spec, slider;
			#val, num, chan, src = args;
			slider = this.varCtrSliders[varSliderIndex];
			spec = slider.controlSpec;
			val = spec.map(val / 127);

			{ this.updateVarSliders (key, varNum, val, indexOffset) }.defer;
		}, ccNum, chan, srcID);
	}

	midiBindSynthSlider { |key, synthIndex, indexOffset, synthSliderIndex, ccNum, chan, srcID|
		^MIDIFunc.cc({ |...args|
			var val, num, chan, src, spec, slider;
			#val, num, chan, src = args;
			slider = this.synthCtrSliders[synthSliderIndex];
			spec = slider.controlSpec;
			val = spec.map(val / 127);

			{ this.updateSynthSliders (key, synthIndex, val, indexOffset) }.defer;
		}, ccNum, chan, srcID);
	}

	startMIDIlearn {
		var varSliderMIDIFuncs = this.varCtrSliders.collect {};
		// store srcData to allow relearning with different input
		var varSliderSrcData = this.varCtrSliders.collect {};
		var varGrouping = this.initVarCtrColorGroups;
		var varKeys = this.varCtr.select { |x,i| i.even };
		var varSliderIndexKeyDict = IdentityDictionary.new;

		var synthSliderMIDIFuncs = this.synthCtrSliders.collect {};
		var synthSliderSrcData = this.synthCtrSliders.collect {};
		var synthGrouping = this.initSynthCtrColorGroups;
		var synthKeys = this.synthCtr.select { |x,i| i.even };
		var synthSliderIndexKeyDict = IdentityDictionary.new;
		var keyOccurences = IdentityDictionary.new, varNum, midiLearnFunc;
		var keyIndex = 0, key;

		varGrouping.do { |group, envirIndex|
			// need occurence number of keys for updating sliders

			group.do { |x, i|
				key = varKeys[keyIndex];
				varNum = keyOccurences[key] ?? { 0 };

				x.isKindOf(SequenceableCollection).if {
					x.do { |item, j| varSliderIndexKeyDict.put(item, [key, j, varNum]) }
				}{
					varSliderIndexKeyDict.put(x, [key, 0, varNum])
				};
				varNum = varNum + 1;
				keyOccurences.put(key, varNum);
				keyIndex = keyIndex + 1;
			};
		};
		keyIndex = 0;

		synthGrouping.do { |group, synthIndex|
			group.do { |x, i|
				x.isKindOf(SequenceableCollection).if {
					x.do { |item, j| synthSliderIndexKeyDict.put(item, [synthKeys[keyIndex], j, synthIndex]) }
				}{
					synthSliderIndexKeyDict.put(x, [synthKeys[keyIndex], 0, synthIndex])
				};
				keyIndex = keyIndex + 1
			}
		};

		midiLearnFunc = MIDIFunc.cc({ |val, num, chan, src|
			{
				var srcData = [num, chan, src];
				this.varCtrSliders.do { |slider, i|
					slider.sliderView.hasFocus.if {
						varSliderMIDIFuncs[i].isKindOf(MIDIFunc).not.if {
							varSliderMIDIFuncs[i] = this.midiBindVarSlider(
								varSliderIndexKeyDict[i][0],
								varSliderIndexKeyDict[i][2],
								varSliderIndexKeyDict[i][1],
								i,
								num,
								chan,
								src
							);
							this.window.onClose_(this.window.onClose <> {
	    						varSliderMIDIFuncs[i].free;
								varSliderMIDIFuncs[i] = nil;
							});
							varSliderSrcData[i] = srcData;
						}{
							(srcData != varSliderSrcData[i]).if {
								varSliderMIDIFuncs[i].free;
								varSliderMIDIFuncs[i] = nil;
								varSliderSrcData[i] = srcData;
							}

						}
					}
				};

				this.synthCtrSliders.do { |slider, i|
					slider.sliderView.hasFocus.if {
						synthSliderMIDIFuncs[i].isKindOf(MIDIFunc).not.if {
							synthSliderMIDIFuncs[i] = this.midiBindSynthSlider(
								synthSliderIndexKeyDict[i][0],
								synthSliderIndexKeyDict[i][2],
								synthSliderIndexKeyDict[i][1],
								i,
								num,
								chan,
								src
							);
							this.window.onClose_(this.window.onClose <> {
	    						synthSliderMIDIFuncs[i].free;
								synthSliderMIDIFuncs[i] = nil;
							});
							synthSliderSrcData[i] = srcData;
						}{
							(srcData != synthSliderSrcData[i]).if {
								synthSliderMIDIFuncs[i].free;
								synthSliderMIDIFuncs[i] = nil;
								synthSliderSrcData[i] = srcData;
							}

						}
					}
				}
			}.defer
		}, nil);
		// use instance variable in next version
		this.addDependant('midiLearnFunc' -> midiLearnFunc)
	}

	stopMIDIlearn {
		// use instance variable in next version
		var assocs = this.dependants.select { |d| d.isKindOf(Association) and: { d.key == 'midiLearnFunc' } };
		assocs.do { |a|
			a.value.free;
			this.dependants.remove(a)
		}
	}

}






