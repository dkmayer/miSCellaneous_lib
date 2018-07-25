
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

VarGui	{
	var <>varCtr, <>synthCtr, <>synthIDs, <>server, <>streams, <>synthCtrIndices,
		<>varCtrNum, <>varNum, <>varColorNum, <>synthCtrNum, <synthPlayerNum, <streamPlayerNum, <>synthColorNum,
		hasConnectedVarEnvirGroups, hasConnectedSynthCtrGroups, firstSynthCtrIndices,
		<>varStrings, varCtrSizes, synthCtrSizes, <>synthCtrStrings, <>saveData, <>envirs,
		<>varEnvirIndices, <synthCtrSynthIndices, <>varEnvirGroups, <synths, <>assumePlaying, <>assumeRunning,
		<>assumeEnded, envirs, streamEnvirs, <streamEnvirIndices, varEnvirIndices, <clocks, <quants,
		<>window, <playerSection, <>colors, <>varCtrColorPairs, <>synthCtrColorPairs, <>synthCtrSliders,
		<>varCtrSliders, <>synthSliderDict, <>varSliderDict, <>synthNameBoxes;

	*new { |varCtr, synthCtr, stream, synth, clock, quant, varEnvirGroups, envir, synthCtrGroups,
			assumePlaying, assumeRunning, assumeEnded, server|
		^super.new.init(varCtr, synthCtr, stream, synth, clock, quant, varEnvirGroups, envir, synthCtrGroups,
			assumePlaying, assumeRunning, assumeEnded, server);
	}
	*load { |pathname, filename, stream, synth, clock, quant, varEnvirGroups, envir, synthCtrGroups,
			assumePlaying, assumeRunning, assumeEnded, server|
		var loadData = (pathname ++ filename).load;
		^super.new.init(loadData[0], loadData[1], stream, synth, clock, quant, varEnvirGroups ?? loadData[2], envir,
			synthCtrGroups ?? loadData[3], assumePlaying, assumeRunning, assumeEnded, server);
	}
	*load_old { |pathname, filename, stream, synth, clock, quant, varEnvirGroups, envir, synthCtrGroups,
			assumePlaying, assumeRunning, assumeEnded, server|
		var loadData = (pathname ++ filename).load;
		^super.new.init(loadData[0], loadData[1].select { |x,i| i.odd }, stream, synth, clock, quant,
			varEnvirGroups ?? loadData[2], envir, synthCtrGroups ?? loadData[3], assumePlaying,
			assumeRunning, assumeEnded, server);
	}
	init { |varCtr, synthCtr, stream, synth, clock, quant, varEnvirGroups, envir, synthCtrGroups,
		assumePlaying, assumeRunning, assumeEnded, server|
		this.server = server ?? { Server.default };

		this.checkArgs(varCtr, synthCtr, stream, synth, varEnvirGroups, envir,
			synthCtrGroups, assumePlaying, assumeRunning, assumeEnded);

		saveData = [
			// varCtr, synthCtr may have been flattened, so store mapping info as groupings
			// miSC_collectCopy for nested arrays with unclear pointer structure
			this.varCtr.miSC_collectCopy,
			this.synthCtr.miSC_collectCopy,
			varEnvirGroups ?? {
				((this.varCtr.size != 0) and: { varEnvirIndices.maxItem > 0 }).if {
					varEnvirIndices.miSC_groupingFromIndices
				}
			},
			synthCtrGroups ?? {
				((synthCtrSynthIndices.size != 0) and: { synthCtrSynthIndices.maxItem > 0 }).if {
					synthCtrSynthIndices.miSC_groupingFromIndices
				}
			}
		];

		this.varCtr.do { |item,i|
			var x, envir;
			(i % 2 == 0).if {
				this.varCtr[i+1].every(_.isSequenceableCollection).if {
					envir = envirs[varEnvirIndices[i.div(2)]];
					x = envir[item];
					varCtrSizes = varCtrSizes.add(this.varCtr[i+1].size);
					// build array in this envir with this name if not already there
					((x.isSequenceableCollection) and: { x.size == (this.varCtr[i+1].size) }).not.if {
						envir[item] = Array.newClear(this.varCtr[i+1].size)
					}
				}{
					varCtrSizes = varCtrSizes.add(1);
				};
			}
		};

		this.synthCtr.do { |item,i|
			i.even.if {
				this.synthCtr[i+1].every(_.isSequenceableCollection).if {
						synthCtrSizes = synthCtrSizes.add(this.synthCtr[i+1].size);
				}{
					synthCtrSizes = synthCtrSizes.add(1);
				};
				synthCtrIndices = synthCtrIndices.add((synths[synthCtrSynthIndices[i.div(2)]]).miSC_getCtrIndex(item));
			}
		};

		// number of variables (keys) counting multiple keys but disregarding arrays:
		varNum = this.varCtr.size.div(2);

		// number of variable controls counting multiple keys and regarding arrays:
		varCtrNum = varCtrSizes.asArray.sum;
		streamPlayerNum = streams.size;

		// number of synth controls counting multiple keys in different synths and regarding arrays:
		synthCtrNum = synthCtrSizes.asArray.sum;
		synthPlayerNum = synths.size;

		clocks = case
			{ clock.isNil }{ streams.collect {|x| x.clock ?? TempoClock.default } }
			{ clock.isSequenceableCollection }{ clock.collect {|x,i| x ?? streams[i].clock ?? TempoClock.default } }
			{ clock.isKindOf(Clock) }{ clock!(streamPlayerNum) };

		quants = case
			{ quant.isNil }{ Quant.default!(streamPlayerNum) }
			{ quant.isSequenceableCollection }{ quant.collect(_.asQuant) }
			{ true }{ quant.asQuant!(streamPlayerNum) };

		^this
	}



	gui { |sliderHeight = 18, sliderWidth, labelWidth = 75, numberWidth = 45,
		sliderType = \standard, sliderMode = \jump, playerHeight = 18, precisionSpan = 10, stepEqualZeroDiv = 100,
		tryColumnNum = 1, allowSynthsBreak = true, allowVarsBreak = true, allowSynthBreak = true,
		allowArrayBreak = true, allowEnvirBreak = true, minPartitionSize = 0,
		sliderPriority = \var, playerPriority = \stream, streamPlayerGroups, synthPlayerGroups, comboPlayerGroups,
		font = "Helvetica", colorsLo, colorsHi, colorDeviation, ctrButtonGreyCenter, ctrButtonGreyDev, greyMode = false,
		varColorGroups, synthColorGroups, name, updateNow = true,
		sliderFontHeight = 12, playerFontHeight = 12, maxWindowWidth = 1500, maxWindowHeight = 700|
		var windowName, tempColors, totalHeight, totalWidth, heightK,
			minSliderFontHeight = 10, maxSliderFontHeight = 16, minFontSliderHeight = 12, maxFontSliderHeight = 24,

			cview, pview, pviewBounds, varColorNum, synthColorNum, colorNum,
			colorDeviationDefault = 0.12, greyModeColorDeviationDefault = 0.0, varColorGroupsWasNil, groups,
			colorsLoDefault = 0.25, greyModeColorsLoDefault = 0.4, colorsHiDefault = 0.7, greyModeColorsHiDefault = 0.6,
			ctrButtonGreyCenterDefault = 0.5, ctrButtonGreyDevDefault = 0.8, greyModeCtrButtonGreyCenterDefault = 0.5,
			greyModeCtrButtonGreyDevDefault = 0.65,

			synthNumWidth = 20, leftMargin = 40, columnIndent = 40,
			rightMargin = 40, upperMargin = 30, lowerMargin = 30, gap = 3,
			maxColumnNum, columnNum, possibleColumnBreakIndices, maxSliderWidth = 270, minSliderWidth = 100,
			columnBreakIndices, effectiveSliderWidth, sliderSectionHeight = 0,

			xPosMin = 100, xPosMax = 500, yPosMin = 100, yPosMax = 500,
			fontColor = Color.black, colorAreaUsage = 0.25, colorSteps = 1000, numColorExp = 0.4, k;

		window.notNil.if { SimpleInitError("Only one gui window from one VarGui at a time").class_(VarGui).throw; };

		// div inits

		varColorGroups.notNil.if { varCtrNum.miSC_checkColorGroups(varColorGroups) };
		synthColorGroups.notNil.if { synthCtrNum.miSC_checkColorGroups(synthColorGroups) };

		colorDeviation = colorDeviation ?? {
			(varColorGroups.notNil || synthColorGroups.notNil).if {
				0
			}{
				greyMode.if { greyModeColorDeviationDefault }{ colorDeviationDefault }
			}
		};


		(greyMode && (varColorGroups.notNil || synthColorGroups.notNil)).if {
			SimpleInitError("If a color grouping is passed greyMode must not be true").throw;
		};

		varColorGroupsWasNil = varColorGroups.isNil;

		// overrule standard color grouping scheme if one color grouping is given
		varColorGroups = varColorGroups ?? {
			groups = this.initVarCtrColorGroups;
			synthColorGroups.isNil.if { groups }{ groups.flatten(1).collect(_.asArray) }
		};
		synthColorGroups = synthColorGroups ?? {
			groups = this.initSynthCtrColorGroups;
			varColorGroupsWasNil.if { groups }{ groups.flatten(1).collect(_.asArray) }
		};


		// check if wslib slider classes are known for respective sliderType
		case
			{ (sliderType == \smooth) }
				{
					\EZSmoothSlider.asClass.isNil.if {
						SimpleInitError("EZSmoothSlider unknown, wslib not installed ?").throw;
					}
				}
			{ (sliderType == \round) }
				{
					\EZRoundSlider.asClass.isNil.if {
						SimpleInitError("EZRoundSlider unknown, wslib not installed ?").throw;
					}
				}
			{ sliderType == \standard }{ }
			{ true }
				{ SimpleInitError("sliderType must be one of \standard, \smooth or \round, "" ++ ""
					last both need wslib installed") };
		case
			{ (sliderMode == \move) }
				{
					(sliderType == \standard).if {
						SimpleInitError("sliderMode \move only with EZSmoothSlider or EZRoundSlider (wslib)").throw;
					}
				}

			{ sliderMode == \jump }{ }
			{ true }
				{ SimpleInitError("sliderType must be one of \jump or \move, "" ++ ""
					latter needs wslib installed") };

		varCtrColorPairs = varCtrNum.miSC_colorDeviationPairs(varColorGroups);
		synthCtrColorPairs = synthCtrNum.miSC_colorDeviationPairs(synthColorGroups);

		maxColumnNum = ((maxWindowWidth - leftMargin - rightMargin + columnIndent) / (minSliderWidth + labelWidth + gap)).floor;
		tryColumnNum = min(tryColumnNum, maxColumnNum).asInteger;

		possibleColumnBreakIndices = this.possibleColumnBreakIndices(
			sliderPriority, allowSynthsBreak, allowVarsBreak, allowSynthBreak, allowArrayBreak, allowEnvirBreak, minPartitionSize);
		columnBreakIndices = (possibleColumnBreakIndices.size <= (tryColumnNum - 1)).if {
			possibleColumnBreakIndices;
		}{
			(tryColumnNum == 1).if { [] }{ (varCtrNum + synthCtrNum).miSC_eqPart(possibleColumnBreakIndices, tryColumnNum) };
		};
		columnNum = (synthCtrNum + varCtrNum == 0).if { 0 }{ columnBreakIndices.size + 1 };

		sliderWidth !? { maxSliderWidth = sliderWidth; minSliderWidth = sliderWidth; };
		effectiveSliderWidth = this.effectiveSliderWidth(columnNum, maxColumnNum, minSliderWidth, maxSliderWidth);

		colorsLo = colorsLo ?? { greyMode.if { greyModeColorsLoDefault }{ colorsLoDefault } };
		colorsHi = colorsHi ?? { greyMode.if { greyModeColorsHiDefault }{ colorsHiDefault } };


		ctrButtonGreyCenter = ctrButtonGreyCenter ?? { greyMode.if { greyModeCtrButtonGreyCenterDefault }{ ctrButtonGreyCenterDefault } };
		ctrButtonGreyDev = ctrButtonGreyDev ?? {
			greyMode.if { greyModeCtrButtonGreyDevDefault }{ ctrButtonGreyDevDefault }
		};

		varColorNum = (this.varCtrColorPairs.collect(_[0]).maxItem ?? - 1) + 1;
		synthColorNum = (this.synthCtrColorPairs.collect(_[0]).maxItem ?? - 1) + 1;
		colorNum = varColorNum + synthColorNum + 2;

		totalWidth = effectiveSliderWidth * columnNum + ((columnNum - 1) * columnIndent) + leftMargin + rightMargin + 3 + 500;

		tempColors = colorNum.miSC_distinctColors(colorsLo, colorsHi, colorsLo, colorsHi, colorsLo, colorsHi, colorAreaUsage, greyMode);

		windowName = this.windowName(name, sliderPriority, playerPriority);
		window = GUI.window.new(windowName, Rect(rrand(xPosMin, xPosMax), rrand(yPosMin, yPosMax), totalWidth, 80), resizable: false).front;

		window.onClose_({
	    		#window, playerSection, colors, varCtrColorPairs, varSliderDict, synthSliderDict,
				synthCtrColorPairs, synthCtrSliders, varCtrSliders, synthNameBoxes = Array.newClear(10);
	    });


		cview = GUI.compositeView.new(window, Rect(0, 0, totalWidth, 80));
		cview.decorator = FlowLayout(cview.bounds, leftMargin @ upperMargin, gap @ gap);

		// build slider section

		sliderSectionHeight = this.placeSliders(sliderPriority, sliderType, cview, columnBreakIndices,
			effectiveSliderWidth, sliderHeight, labelWidth, numberWidth, synthNumWidth, tempColors, numColorExp,
			columnIndent, gap, leftMargin, varColorNum, synthColorNum, colorDeviation, greyMode, fontColor);

    		heightK = (maxSliderFontHeight - minSliderFontHeight) / (maxFontSliderHeight - minFontSliderHeight);
    		sliderFontHeight = sliderFontHeight ?? case
    			{ sliderHeight < minFontSliderHeight }{ minSliderFontHeight }
    				{ sliderHeight > maxFontSliderHeight }{ maxSliderFontHeight }
    			{ true } { (sliderHeight - minFontSliderHeight) * heightK + minSliderFontHeight };
   		cview.decorator.reset;
		cview.decorator.left_((effectiveSliderWidth + columnIndent) * columnNum + leftMargin);
    		cview.decorator.shift(y: gap);

    		pviewBounds =  Rect(0, 0, 450, 1000);
    		pview = CompositeView(cview, pviewBounds);
    		pview.decorator = FlowLayout(pviewBounds, 0 @ 0);
    		pview.background_(Color.new255(0, 150, 150, 0));

		// build player section

 		playerSection = try { VarGuiPlayerSection.new(this, pview, playerHeight, streamPlayerGroups, synthPlayerGroups, comboPlayerGroups,
			ctrButtonGreyCenter, ctrButtonGreyDev, fontColor, playerPriority);
 		}{
	 		|error|
	 		{ window.close }.defer(0.5);
	 		error.throw;
 		};

		totalHeight = max(sliderSectionHeight, playerSection.totalHeight) + lowerMargin + upperMargin;
		totalWidth = effectiveSliderWidth * columnNum + (columnNum * columnIndent) + playerSection.totalWidth + leftMargin + rightMargin + 3;

		cview.bounds = Rect(0, 0, totalWidth, totalHeight);

		cview.background = tempColors[colorNum - [1,2].choose];
		// if Gradient will be reimplemented in qt:
		// Gradient(tempColors[colorNum - 1], tempColors[colorNum - 2], \v, colorSteps);

		window.bounds = Rect(rrand(xPosMin, xPosMax), rrand(yPosMin, yPosMax), totalWidth, totalHeight);
    		this.font(font, sliderFontHeight, playerFontHeight);

		[varCtrSliders, synthCtrSliders].do { |sliders|
			sliders.do { |ez|
				ez.miSC_adaptToControlStep(precisionSpan, stepEqualZeroDiv).miSC_mode_(sliderMode);
			}
		};

		this.initSynthSliderDict;
 		this.initVarSliderDict;

		updateNow.if { this.update };
    	^this
	}

	update {
		varCtrSliders.do({|item| item.action.value(item) });
		synthCtrSliders.do({|item| item.action.value(item) });
		^this
	}

	font {|font = "Helvetica", sliderFontHeight = 16, playerFontHeight = 12|
		var x;
		varCtrSliders.do({|item|
			item.labelView.font_(Font(font, sliderFontHeight));
			item.numberView.font_(Font(font, sliderFontHeight));
		});
		synthCtrSliders.do({|item|
			item.labelView.font_(Font(font, sliderFontHeight));
			item.numberView.font_(Font(font, sliderFontHeight));
		});
		synthNameBoxes.do({|item|
			item.font_(Font(font, sliderFontHeight)).refresh;
		});

		(x = playerSection.mouseActionButton) !?  { x.font_(Font(font, playerFontHeight)); x.refresh };
		(x = playerSection.envirIndexButton) !?  { x.font_(Font(font, playerFontHeight)); x.refresh };
		(x = playerSection.latencyButton) !?  { x.font_(Font(font, playerFontHeight)); x.refresh };

		playerSection.basicNewViews.do(_.font_(Font(font, playerFontHeight)));
		(x = playerSection.modeNameView) !?  { x.font_(Font(font, playerFontHeight)); x.refresh };

		playerSection.streamEnvirViews.do(_.font_(Font(font, playerFontHeight)));
		playerSection.streamTypeTexts.do(_.font_(Font(font, playerFontHeight)));
		playerSection.synthRateTexts.do(_.font_(Font(font, playerFontHeight)));

		playerSection.optionViews.do(_.font_(Font(font, playerFontHeight)));
		playerSection.optionViewTexts.do(_.font_(Font(font, playerFontHeight)));

		(x = playerSection.saveButton) !?  { x.font_(Font(font, playerFontHeight)); x.refresh };
		(x = playerSection.updateButton) !? { x.font_(Font(font, playerFontHeight)); x.refresh };
		(x = playerSection.stopButton) !? { x.font_(Font(font, playerFontHeight)); x.refresh };

		^this
	}


	windowName {|name, sliderPriority, playerPriority|
		var a,b;
		^(name ?? {
			a = case
				{ (varCtrNum != 0) && (synthCtrNum != 0) }
					{ (sliderPriority == \var).if { "variable and synth control" }{ "synth and variable control" } }
				{ (varCtrNum == 0) && (synthCtrNum != 0) } { "synth control" }
				{ (varCtrNum != 0) && (synthCtrNum == 0) } { "variable control" }
				{ (varCtrNum == 0) && (synthCtrNum == 0) } { "no control" };
			b = case
				{ (streamPlayerNum != 0) && (synthPlayerNum != 0) }
					{ (playerPriority == \stream).if { "stream and synth players" }{ "synth and stream players" } }
				{ (streamPlayerNum == 0) && (synthPlayerNum != 0) }
					{ "synth player" ++ ((synthPlayerNum != 1).if { "s" }{ "" }) }
				{ (streamPlayerNum != 0) && (synthPlayerNum == 0) }
					{ "stream player" ++ ((streamPlayerNum != 1).if { "s" }{ "" }) }
				{ (streamPlayerNum == 0) && (synthPlayerNum == 0) } { "no players" };
			a ++ "    |    " ++ b;
		}).asString;
	}

	initVarCtrColorGroups { // logical variable control color grouping
		var j = 0, col, k;
		col = Array.newClear((varEnvirIndices.size != 0).if {
			varEnvirIndices.maxItem + 1
		}{
			0
		});

		varCtr.do {|x,i|
			i.odd.if {
				k = varEnvirIndices[i.div(2)];
				x[0].isSequenceableCollection.if {
					col[k] = col[k].add(x.collect {|y| j = j + 1; j - 1 });
				}{
					col[k] = col[k].add(j);
					j = j + 1;
				}
			}
		};
		// special case there could be nils if some synths without control
		^col.reject { |x| x.isNil }
	}

	initSynthCtrColorGroups { // logical synth control color grouping
		var j = 0, col, k;

		col = Array.newClear((synthCtrSynthIndices.size != 0).if {
			synthCtrSynthIndices.maxItem + 1
		}{
			0
		});

		synthCtr.do {|x,i|
			i.odd.if {
				k = synthCtrSynthIndices[i.div(2)];
				x[0].isSequenceableCollection.if {
					col[k] = col[k].add(x.collect {|y| j = j + 1; j - 1 });
				}{
					col[k] = col[k].add(j);
					j = j + 1;
				}
			}
		};
		// special case there could be nils if some synths without control
		^col.reject { |x| x.isNil }
	}

	initSynthSliderDict {
		var dict = MultiLevelIdentityDictionary.new;
		var synthNum = this.synthCtrSynthIndices.asSet, relSynthCtrIndices,
			sliderIndex = 0, key, size;
		synthNum.do { |synthIndex|
			relSynthCtrIndices = this.synthCtrSynthIndices.findAll([synthIndex]);
			relSynthCtrIndices.do { |i|
				key = synthCtr[i * 2];
				size = synthCtrSizes[i];
				dict.put(synthIndex, key, \size, size);
				dict.put(synthIndex, key, \sliderIndex, sliderIndex);
				sliderIndex = sliderIndex + size;
			}
		};
		this.synthSliderDict = dict;
	}

	initVarSliderDict {
		var dict = MultiLevelIdentityDictionary.new;
		var sliderIndex = 0, size;
		varCtr.pairsDo { |key, spec, i|

			size = varCtrSizes[i.div(2)];
			dict[key, \varOccurrences].isNil.if {
				dict.put(key, \varNum, 0, \sliderIndex, sliderIndex);
				dict.put(key, \varNum, 0, \size, size);
				dict.put(key, \varOccurrences, 1);
			}{
				dict.put(key, \varNum, dict[key, \varOccurrences], \sliderIndex, sliderIndex);
				dict.put(key, \varNum, dict[key, \varOccurrences], \size, size);
				dict.put(key, \varOccurrences, dict[key, \varOccurrences] + 1);
			};
			sliderIndex = sliderIndex + size;
		};
		this.varSliderDict = dict;
	}
}

