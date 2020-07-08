
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

+VarGui	{

	performVarArrayActions {|i, k, offset, symbol, value|
		var clippedValue;
		varCtrSliders[offset + k].value_(value);
		clippedValue = varCtrSliders[offset + k].value;
		envirs.at(varEnvirIndices[i.div(2)]).at(symbol).put(k, clippedValue);
     	(saveData[0][i][k]).put(4, clippedValue);
	}

	performSynthArrayActions {|j, k, offset, value|
		var clippedValue;
		synthCtrSliders[offset + k].value_(value);
		clippedValue = synthCtrSliders[offset + k].value;
		(saveData[1][j][k]).put(4, clippedValue);
	}


	performSliderActions {|symbol, value, reg, mod, symbolIndex, arrayIndex|
		// if reg == \var: symbolIndex = index of var
		// if reg == \synth: symbolIndex = index of synth

		var msgList, ii, k = 0, sliderOffset = 0, modType, collectType, ctrIndex, synthIndex, clippedValue;

		modType = case
			{ (mod.isShift) && (mod.isCtrl) }{ 0 }
			{ (mod.isShift)  }{ 1 }
			{ true }{ 2 };

		((reg == \var) || (mod.miSC_isPseudoCaps)).if {
			forBy(0, saveData[0].size - 2, 2, {|i|
				ii = i.div(2);
				(saveData[0][i] == symbol).if {
					(((symbolIndex == ii) && (reg == \var)) || (mod.isAlt)).if {
						arrayIndex.isNil.if {
							varCtrSliders[sliderOffset].value_(value);
							clippedValue = varCtrSliders[sliderOffset].value;
							envirs.at(varEnvirIndices[ii]).put(symbol, clippedValue);
      	  					(saveData[0][i+1]).put(4, clippedValue);
						}{
							saveData[0][i+1].do {|x,k|
								modType.switch(
									0, { (k <= arrayIndex).if { this.performVarArrayActions(i+1, k, sliderOffset, symbol, value) } },
									1, { (k >= arrayIndex).if { this.performVarArrayActions(i+1, k, sliderOffset, symbol, value) } },
									2, { (arrayIndex == k).if { this.performVarArrayActions(i+1, k, sliderOffset, symbol, value) } }
								);
							}											}
					}
				};
				sliderOffset = sliderOffset + saveData[0][i+1][0].isSequenceableCollection.if { saveData[0][i+1].size }{ 1 };
			})
		};

		sliderOffset = 0;

		((reg == \synth) || (mod.miSC_isPseudoCaps)).if {
			forBy(0, saveData[1].size - 2, 2, {|j|
				(saveData[1][j] == symbol).if {

					synthIndex = synthCtrSynthIndices[j.div(2)];

					(((symbolIndex == j.div(2)) && (reg == \synth)) || (mod.isAlt)).if {
						arrayIndex.isNil.if {
							synthCtrSliders[sliderOffset].value_(value);
							clippedValue = synthCtrSliders[sliderOffset].value;
      	  					[-1,2].includes(playerSection.synthStates[synthIndex]).not.if {
								msgList = msgList.add([\n_set, (synthIDs[synthIndex]).asNodeID, symbol, clippedValue]);
							};
							(saveData[1][j+1]).put(4, clippedValue);
						}{
							saveData[1][j+1].do {|x,k|
								modType.switch(
									0, { (k <= arrayIndex).if { this.performSynthArrayActions(j+1, k, sliderOffset, value) } },
									1, { (k >= arrayIndex).if { this.performSynthArrayActions(j+1, k, sliderOffset, value) } },
									2, { (arrayIndex == k).if { this.performSynthArrayActions(j+1, k, sliderOffset, value) } }
								);
							};

							[-1,2].includes(playerSection.synthStates[synthIndex]).not.if {
								ctrIndex = synthCtrIndices[j.div(2)];
								collectType = modType.switch(
									0, { 1 },
									1, { ctrIndex.notNil.if { 2 }{ 3 } },
									2, { ctrIndex.notNil.if { 0 }{ 1 } }
								);
								msgList = msgList.add(this.makeMsgList(synthIDs[synthIndex], symbol, ctrIndex, arrayIndex, collectType, value, j+1));
							};

						}
					}
				};
				sliderOffset = sliderOffset + saveData[1][j+1][0].isSequenceableCollection.if { saveData[1][j+1].size }{ 1 };
			})
		};

		server.listSendBundle(nil, msgList);
		^this;
	}

	makeMsgList {|synth, argName, ctrIndex, arrayIndex, collectType, value, saveDataIndex|
		// synth may be Synth or nodeID
		// collectType expects \up, \down or \this (from arrayIndex)

		var msgList, size = (saveData[1][saveDataIndex]).size;

		// 0: \this && (ctrIndex.notNil) - this
		// 1: \down - this + down
		// 2: \up && (ctrIndex.notNil) - this + up
		// 3: \up && (ctrIndex.isNil) - this + up + down

		collectType.switch(
			0, { ^[\n_set, synth.asNodeID, ctrIndex + arrayIndex, value] },
			1, { ^[\n_setn, synth.asNodeID, argName, arrayIndex + 1] ++ saveData[1][saveDataIndex][0..arrayIndex].collect(_.at(4)) },
			2, { ^[\n_setn, synth.asNodeID, ctrIndex + arrayIndex, size - arrayIndex] ++ saveData[1][saveDataIndex][arrayIndex..(size-1)].collect(_.at(4)) },
			3, { ^[\n_setn, synth.asNodeID, argName, size] ++ saveData[1][saveDataIndex][0..(size-1)].collect(_.at(4)) }
		);
	}

	//
	// updateVarSliders {|key, varNum = 0, val, indexOffset = 0, updateNow = true|
	// 	// varNum: multiple occurence index, val may be sequenceable collection
	// 	var varStartIndex, sliderStartIndex, slider;
	// 	varStartIndex = this.varCtr.select({|x,i| i.even}).findAll([key]).at(varNum);
	// 	sliderStartIndex = varCtrSizes.keep(max(0, varStartIndex)).sum;
	// 	val.asArray.do {|x,i|
	// 		slider = varCtrSliders[sliderStartIndex + indexOffset + i];
	// 		slider.value = x;
	// 		updateNow.if { slider.action.value(slider) }
	// 	};
	// 	^this
	// }
	//
	// updateSynthSliders {|key, synthIndex, val, indexOffset = 0, updateNow = true|
	// 	// val may be sequenceable collection
	// 	var keyStartIndex, sliderStartIndex, slider;
	// 	keyStartIndex = this.synthCtr.select({|x,i| i.even}).findAll([key])
	// 	.select { |k| synthCtrSynthIndices[k] == synthIndex }.first;
	// 	sliderStartIndex = synthCtrSizes.keep(max(0, keyStartIndex)).sum;
	// 	val.asArray.do {|x,i|
	// 		slider = synthCtrSliders[sliderStartIndex + indexOffset + i];
	// 		slider.value = x;
	// 		updateNow.if { slider.action.value(slider) }
	// 	};
	// 	^this
	// }

	updateSynthSliders {|key, synthIndex, val, indexOffset = 0, updateNow = true|
		// val may be sequenceable collection
		var sliderStartIndex, slider;
		sliderStartIndex = synthSliderDict[synthIndex, key, \sliderIndex];

		val.asArray.do {|x,i|
			slider = synthCtrSliders[sliderStartIndex + indexOffset + i];
			slider.value = x;
			updateNow.if { slider.action.value(slider) }
		};
		^this
	}

	updateVarSliders {|key, varNum = 0, val, indexOffset = 0, updateNow = true|
		// varNum: multiple occurence index, val may be sequenceable collection
		var sliderStartIndex, slider;
		sliderStartIndex = varSliderDict[key, \varNum, varNum, \sliderIndex];

		val.asArray.do {|x,i|
			slider = varCtrSliders[sliderStartIndex + indexOffset + i];
			slider.value = x;
			updateNow.if { slider.action.value(slider) }
		};
		^this
	}

	makeSaveValueMsgList {|synthIndex|
		var msgList;
		saveData[1].do {|item, j|
			j.even.if {
				(synthCtrSynthIndices[j.div(2)] == synthIndex).if {
					saveData[1][j+1][0].isSequenceableCollection.not.if {
						msgList = msgList.add([\n_set, (synthIDs[synthIndex]).asNodeID,
							saveData[1][j], saveData[1][j+1][4]]);
					}{
						msgList = msgList.add(this.makeMsgList(synthIDs[synthIndex], item, nil,
							saveData[1][j+1].size - 1, 1, saveData[1][j+1].last.at(4), j+1));
					}
				}
			}
		};
		^msgList
	}

}

