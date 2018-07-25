
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


VarGuiPlayerSection {
	var <>varGui, <>streamPlayerGroups, <>synthPlayerGroups, <>comboPlayerGroups, synths, <isWatched,
		<basicNewViews, synthViewGroups, synthButtonGroups, <synthStates, synthActionStates,
		makeSynthResponders, synthResponders, synthDefSymbols, sourceTypes, rates, synthPlayActions,
		synthPauseActions, synthStopActions, synthEndActions,
		synthRenewModeStates, synthRenewModeViews, synthRenewModeButtons, synthStopModeStates, synthStopModeViews,
		synthStopModeButtons, synthStopModeActions, synthRenewModeActions, <synthRateTexts,
		parentHelpSynths,

		streamViewGroups, <streamEnvirViews, streamButtonGroups, streamStates, streamSimpleControllers, streamPlayActions,
		streamPauseActions, streamResetActions, streamResetModeStates, streamResetModeViews, streamResetModeButtons,
		streamResetModeActions, <streamTypeTexts,

		stopByCmdPeriodActions, onCloseFunctions, playerGuiAction, notificationIsOn, notificationWasOff,
		mouseDownOn = true, showEnvirIndex, customLatency,
		modifier,

		buttonHiColor, buttonLoColor, playBackgroundColor, playPlayerButtonColor, pauseBackgroundColor, pausePlayerButtonColor,
		stopBackgroundColor, stopPlayerButtonColor, resetBackgroundColor, resetPlayerButtonColor,
		synthBasicNewColor, liteGreyBackgroundColor, darkGreyBackgroundColor,
		fontColor, modeButtonColor,

		optionHiColor, optionLoColor,

		decoratorGapX, decoratorGapY,

		fieldHeight, fieldWidth, rectOut, rectIn, basicNewRect, smallRectOut, modeRectOut, modeRectIn, smallRectIn,
		modeRectOutTop, modeRectOutBottom, modeNameRect, modeRectScale, smallRectScale,
		buttonGapH, buttonGapW, columnGroupGap, columnGroupGapFactor, optionViewWidth, <totalWidth, <totalHeight,
		optionViewsGapTop, optionViewsGapBottom,

		optionViewMakers, <optionViews, <optionViewTexts, optionViewNum = 0, <mouseActionButton, <envirIndexButton, <latencyButton,
		latencies, latencyIndex, customLatencyBox, serverLatencyBox,

		<updateButton, <saveButton, <stopButton, globalButtonHeight, globalButtonWidth, globalButtonGap, indexWidth,
		updateButtonColor, saveButtonColor, stopButtonColor,

		<>ctrButtonGreyCenter, <>ctrButtonGreyDev, <>playerPriority, placeSynthPlayers, placeStreamPlayers,
		iconViewGroup, makeIconViewGroup, <modeNameView,
		serverLatency;

	*new { |varGui, playerView, playerHeight, streamPlayerGroups, synthPlayerGroups, comboPlayerGroups,
			ctrButtonGreyCenter, ctrButtonGreyDev, fontColor, playerPriority|
		^super.new.init(varGui, playerView, playerHeight, streamPlayerGroups, synthPlayerGroups,
			comboPlayerGroups, ctrButtonGreyCenter, ctrButtonGreyDev, fontColor, playerPriority);
	}


	updateSynth { |i|
		this.varGui.synthIDs[i] = synths[i].asNodeID;
		makeSynthResponders[i].(synths[i]);
		^this
	}


	checkPlayerGroups { |groups, num, errorString|
		groups.notNil.if {
			groups.miSC_isGrouping(num).if {
				^groups;
			}{
				SimpleInitError(errorString).class_(VarGui).throw;
			}
		}{
			^[(0..(num - 1))];
		};
	}


	init { |varGui, playerView, playerHeight, streamPlayerGroups, synthPlayerGroups, comboPlayerGroups,
			ctrButtonGreyCenter, ctrButtonGreyDev, fontColor, playerPriority|
		var grey;

		this.varGui = varGui;

		this.synthPlayerGroups = this.checkPlayerGroups(synthPlayerGroups, varGui.synthPlayerNum,
			"synthPlayerGroups must be valid grouping of number of synth players," ++
			" e.g. [[2,3], [0], [4,1]] in case of 5 players");


		this.streamPlayerGroups = this.checkPlayerGroups(streamPlayerGroups, varGui.streamPlayerNum,
			"streamPlayerGroups must be valid grouping of number of stream players," ++
			" e.g. [[2,3], [0], [4,1]] in case of 5 players");


		this.comboPlayerGroups = this.checkPlayerGroups(comboPlayerGroups, varGui.synthPlayerNum + varGui.streamPlayerNum,
			"comboPlayerGroups must be valid grouping of total number of synth and stream players," ++
			" e.g. [[2,3], [0], [4,1]] in case of 5 players");

		this.ctrButtonGreyCenter = ctrButtonGreyCenter;
		this.ctrButtonGreyDev = ctrButtonGreyDev;
		this.playerPriority = playerPriority;

		grey = (thisProcess.platform.name == \osx).if { 130 }{ 140 };

		darkGreyBackgroundColor = Color.new255(grey, grey, grey);
		liteGreyBackgroundColor = Color.new255(grey, grey, grey);

		buttonHiColor = Color.yellow.miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);
		buttonLoColor = liteGreyBackgroundColor;

		playBackgroundColor = Color.green.miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);
		playPlayerButtonColor = Color.green.miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);

		pauseBackgroundColor = Color.new255(255, 150, 0).miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);
		pausePlayerButtonColor = Color.new255(255, 150, 0).miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);

		stopBackgroundColor = Color.red.miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);
		stopPlayerButtonColor = Color.red.miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);

		resetBackgroundColor = Color.blue.miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);
		resetPlayerButtonColor = Color.blue.miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);

		synthBasicNewColor = Color.new255(250, 250, 250).miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);

		modeButtonColor = Color.grey.miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);

		updateButtonColor = Color.new255(125, 145, 245).miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);
		saveButtonColor = Color.new255(0, 245, 245).miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);
		stopButtonColor = Color.new255(255, 75, 75).miSC_dim(ctrButtonGreyCenter, ctrButtonGreyDev);

		optionLoColor = liteGreyBackgroundColor;
		optionHiColor = synthBasicNewColor;

		decoratorGapX = 4;
		decoratorGapY = 3;

		playerView.decorator.gap = Point(decoratorGapX, decoratorGapY);

		fieldHeight = playerHeight;
		fieldWidth = 40;
		buttonGapH = 2;
		buttonGapW = 10;
		indexWidth = 25;
		columnGroupGapFactor = 2.2;

		columnGroupGap = buttonGapW * columnGroupGapFactor;
		rectOut = Rect(0, 0, fieldWidth, fieldHeight);
		rectIn = Rect(buttonGapW, buttonGapH, fieldWidth - (2 * buttonGapW), fieldHeight - (2 * buttonGapH));
		basicNewRect = Rect(0, 0, indexWidth, fieldHeight);

		modeRectScale = 0.75;
		smallRectScale = 0.75;
		modeRectOut = Rect(0, 0, modeRectScale * fieldWidth, fieldHeight);
		smallRectOut = Rect(0, 0, smallRectScale * fieldWidth, fieldHeight);
		modeRectOutTop = Rect(modeRectOut.left, modeRectOut.top, modeRectOut.width, modeRectOut.height / 2);
		modeRectOutBottom = Rect(modeRectOut.left, modeRectOut.top + modeRectOut.height / 2, modeRectOut.width, modeRectOut.height / 2);
		modeRectIn = Rect(buttonGapW * modeRectScale, buttonGapH, modeRectOut.width - (2 * buttonGapW * modeRectScale), fieldHeight - (2 * buttonGapH));
		modeNameRect = Rect(0, 0, 2 * modeRectOut.width + decoratorGapX, fieldHeight);

		optionViewWidth = (basicNewRect.width) + (rectOut.width * 3) + columnGroupGap + (modeRectOut.width * 2) + (decoratorGapX * 5);
		optionViewsGapTop = fieldHeight * 0.75;
		optionViewsGapBottom = fieldHeight * 0.9;

		totalWidth = optionViewWidth + columnGroupGap + smallRectOut.width + decoratorGapX;
		totalHeight = 0;

		totalHeight = totalHeight +
			case
				{ (varGui.synths.size != 0) || (varGui.streams.size != 0) }
					{ (varGui.synths.size + varGui.streams.size + 1) * (fieldHeight + decoratorGapY) +
						(decoratorGapY * (((varGui.synths.size != 0) && (varGui.streams.size != 0)).if { 2 }{ 1 }))  }
				{ true }{ 0 };

		globalButtonHeight = fieldHeight * 1.35;
		globalButtonWidth = fieldWidth;

		#basicNewViews, synthViewGroups, synthButtonGroups, synthStates, synthActionStates, isWatched,
			makeSynthResponders, synthResponders, synthDefSymbols, sourceTypes, rates, synthPlayActions,
			synthPauseActions, synthStopActions, synthEndActions, synthRenewModeStates, synthRenewModeViews,
			synthRenewModeButtons, synthStopModeStates, synthStopModeViews, synthStopModeButtons, synthStopModeActions,
			synthRenewModeActions, synths, synthRateTexts, parentHelpSynths = Array.newClear(varGui.synthPlayerNum) ! 26;
		#streamViewGroups, streamEnvirViews, streamButtonGroups, streamStates, streamSimpleControllers, streamPlayActions, streamPauseActions,
			streamResetActions, streamResetModeStates, streamResetModeViews, streamResetModeButtons,
			streamResetModeActions, streamTypeTexts = Array.newClear(varGui.streamPlayerNum) ! 13;
		#stopByCmdPeriodActions, onCloseFunctions = List.new ! 2;

		varGui.synths.do({|x,i|
			var watchAll;
			case
				{ x.isKindOf(Integer) } {
					synths[i] = x;
					sourceTypes[i] = \id;

					synthStates[i] = case
						{ (varGui.assumeEnded[i] == true) }{ 2 }
						{ (varGui.assumePlaying[i].isNil || varGui.assumeRunning[i].isNil) }
							{ SimpleInitError("Incomplete synth state information: if nodeID given and assumeEnded not equal true then assumePlaying and assumeRunning " ++
								"must be set to false / true.").class_(VarGui).throw; }
						// don't handle stateless players
						// if synth given as nodeID assumptions must be correct ! - users responsibility

						{ varGui.assumePlaying[i] && varGui.assumeRunning[i] }{ 0 }
						{ varGui.assumePlaying[i] && varGui.assumeRunning[i].not }{ 1 }
						{ varGui.assumePlaying[i].not && varGui.assumeRunning[i].not }{ 2 }
						{ varGui.assumePlaying[i].not && varGui.assumeRunning[i] }
							{ SimpleInitError("Contradiction in synth state given bei assumptions, there must not be a synth that is running and not playing.").class_(VarGui).throw; };

					synthRenewModeStates[i] = false;
				}
				{ x.isKindOf(Synth) } {
					sourceTypes[i] = \obj;
					synths[i] = x;
					parentHelpSynths[i] = x.miSC_getParentHelpSynth;

					synthDefSymbols[i] = x.defName.asSymbol;
					// temporary synth maybe registered but is not renewable
					synthRenewModeStates[i] = ((synthDefSymbols[i].asString.keep(6) == "temp__").if { false }{ true });

					watchAll = NodeWatcher.all[varGui.server.name];
					isWatched[i] = ((watchAll !? { watchAll.value.nodes[x.nodeID] } ) === x);

					synthStates[i] = case
						{ (varGui.assumeEnded[i] == true) }{ 2 }

						// don't handle stateless players
						// if assumptions are nil then believe Synth's isPlaying/is Running if watched by NodeWatcher, else throw Error

						{ (isWatched[i].not && (varGui.assumePlaying[i].isNil || varGui.assumeRunning[i].isNil)) }
							{ SimpleInitError("Incomplete synth state information: if Synth is not registered assumePlaying / assumeRunning " ++
								" (or assumeEnded) must be set to false / true.").class_(VarGui).throw; }

						{ (varGui.assumePlaying[i] ?? { x.isPlaying }) && (varGui.assumeRunning[i] ?? { x.isRunning }) }{ 0 }
						{ (varGui.assumePlaying[i] ?? { x.isPlaying }) && (varGui.assumeRunning[i] ?? { x.isRunning }).not }
							// can only assume newPaused if synth from HS and flag hasJustStartedWithPause = true
							{ (parentHelpSynths[i].notNil and: { parentHelpSynths[i].hasJustStartedWithPause }).if { 3 }{ 1 } }

						// indicate basic new by assumeEnded = false and isPlaying/isRunning = false

						{ (varGui.assumePlaying[i] ?? { x.isPlaying }).not && (varGui.assumeRunning[i] ?? { x.isRunning }).not && (varGui.assumeEnded[i] == false) }{ -1 }
						{ (varGui.assumePlaying[i] ?? { x.isPlaying }).not && (varGui.assumeRunning[i] ?? { x.isRunning }).not }{ 2 }
						{ (varGui.assumePlaying[i] ?? { x.isPlaying }).not && (varGui.assumeRunning[i] ?? { x.isRunning }) }
							{ SimpleInitError("Contradiction in synth state, there must not be a synth that is running and not playing, may come from " ++
								"a Synth with contradictory state in itself or assumptions that are only partially overriding this wrong state").class_(VarGui).throw; }
				}
				{ x.isKindOf(String) || x.isKindOf(Symbol) }{
					synthDefSymbols[i] = x.asSymbol;
					sourceTypes[i] = \def;
					synths[i] = Synth.basicNew(x, varGui.server);
					synthRenewModeStates[i] = true;
					synthStates[i] = -1;
				}
				{ true } { SimpleInitError("synth source must be synth (object or nodeID) or synth definition (symbol or string)").class_(VarGui).throw; };
		});

		// differentiate between synthStates (last state) and synthActionStates (current action)
		// notification actions come after button actions

		// synthStates:
		// -1: not yet playing (basic new)
		// 0: playing, running
		// 1: playing, not running
		// 2: not playing any more
		// 3: new paused

		// synthActionStates depend on msg bundles
		// 0: new
		// 1: run true
		// 2: run false
		// 3: free
		// 4: new + run false
		// 5: new + free
		// 6: free + new
		// 7: free + new + run false
		// 8: none

		placeSynthPlayers = {
    		synths.do({|x,i|
			var desc, outer, outputs;

			basicNewViews[i] = StaticText(playerView, basicNewRect).string_("  " ++ i.asString)
				.background_(liteGreyBackgroundColor).stringColor_(fontColor);
			synthViewGroups[i] = Array.fill(3, { CompositeView(playerView, rectOut) });
			synthViewGroups[i].do(_.background = buttonLoColor);


			// synths are set to saveData values in case of renew by ( .makeSaveValueMsgList )
			// not necessarily equal to slider values if still initial without update !

			// resetting and assigning controllers done either with reset or play / pause (from state basic new)

			synthPlayActions[i] = { |b|
				synthActionStates[i] = 0;
				case
					{ synthStates[i] == -1 }
						{
							synthActionStates[i] = 0;
							this.updateSynth(i);
							[(synths[i]).newMsg].addAll(varGui.makeSaveValueMsgList(i))
						}
					{ [1,3].includes(synthStates[i]) }
						{
							synthActionStates[i] = 1;
							[(synths[i]).miSC_runMsg(true)].addAll((synthStates[i] == 3).if { varGui.makeSaveValueMsgList(i) }{ [] })
						}
					{ true }{ synthActionStates[i] = 8; nil };
			};
			synthPauseActions[i] = { |b|
				synthActionStates[i] = 1;
				case
					{ synthStates[i] == -1  }
						{
							synthActionStates[i] = 4;
							this.updateSynth(i);
							[(synths[i]).newMsg, (synths[i]).miSC_runMsg(false)];
						}
					{ synthStates[i] == 0 }{ synthActionStates[i] = 2; [synths[i].miSC_runMsg(false)] }
					{ true }{ synthActionStates[i] = 8; nil };
			};




			synthStopActions[i] = { |b|
				var oldSynth, msgList = List.new;

				(synthStates[i] == -1).if { // stop basic new
					(synthRenewModeStates[i] && (synthStopModeButtons[i].value != 2)).if {
						synthActionStates[i] = 5;
						this.updateSynth(i);
						msgList.add(synths[i].newMsg).add(synths[i].miSC_freeMsg);
					}{	// "renew" basic new
						(synthRenewModeButtons[i].value).switch(
							0, { synthActionStates[i] = 0; this.updateSynth(i); msgList.add(synths[i].newMsg)
								.addAll(varGui.makeSaveValueMsgList(i)) },
							1, { synthActionStates[i] = 4; this.updateSynth(i); msgList.add(synths[i].newMsg)
								.add(synths[i].miSC_runMsg(false)).addAll(varGui.makeSaveValueMsgList(i)) },
							2, { synthActionStates[i] = 8; /*already basic new, do nothing*/ }
						);
					};
				};
				((synthStates[i] == 0) && (synthStopModeButtons[i].value == 2) && (synthRenewModeButtons[i].value == 0)).if {
					// flash if renew while playing
					{ this.playerGuiAction(synthViewGroups[i], 2, \thisHi); }.defer;
					{ this.playerGuiAction(synthViewGroups[i], 2, \thisLo); }.defer(0.1);
				};

				// real stop or stop to prepare renew
				([0,1,3].includes(synthStates[i])).if { synthActionStates[i] = 3; msgList.add(synths[i].miSC_freeMsg); };

				// renew condition
				(((synthStates[i] == 2) && (synthStopModeButtons[i].value != 0) && synthRenewModeStates[i])
					|| (([0,1,3].includes(synthStates[i])) && (synthStopModeButtons[i].value == 2))).if {

						synthActionStates[i] = 3;
						synthResponders[i].do({ |x| x.disable; x.remove });

						synths[i] = Synth.basicNew(synthDefSymbols[i]).register;
						varGui.synthIDs[i] = synths[i].nodeID;

						synthRenewModeButtons[i].value.switch(
							0, { synthActionStates[i] = (synthActionStates[i] == 3).if { 6 }{ 0 };
								makeSynthResponders[i].(synths[i]);
								msgList.add(synths[i].newMsg).addAll(varGui.makeSaveValueMsgList(i)) },
							1, { synthActionStates[i] = (synthActionStates[i] == 3).if { 7 }{ 4 };
								makeSynthResponders[i].(synths[i]);
								msgList.add(synths[i].newMsg).add(synths[i].miSC_runMsg(false)).addAll(varGui.makeSaveValueMsgList(i)) },
							2, {
								{ // basicNew - gui and state update to be done by button action itself as no notification
									basicNewViews[i].background = synthBasicNewColor;
									this.playerGuiAction(synthViewGroups[i], 0, \allLo);
									(synthStopModeButtons[i].value != 2).if {
										(synthButtonGroups[i][2]).states_([["", fontColor, stopPlayerButtonColor]]).refresh;
									};
									synthActionStates[i] = 8;
									synthStates[i] = -1;
								}.defer;
							}
						);
					};
					msgList;
			};

			makeSynthResponders[i] = {|y|
				synthResponders[i] = [
						// OSCpathResponder(varGui.server.addr, ['/n_go', y.asNodeID], { |time, resp, msg|
						// 	// handles renew actions, play from basic new
						// 	// pause from basic new
						// 	// new and free + new
						// 	([0,6].includes(synthActionStates[i])).if {
						// 		{
						// 			(synthStopModeButtons[i].value != 2).if {
						// 				(synthButtonGroups[i][2]).states_([["", fontColor, stopPlayerButtonColor]]).refresh;
						// 			};
						// 			basicNewViews[i].background = liteGreyBackgroundColor;
						//
						// 			this.playerGuiAction(synthViewGroups[i], 0, \thisHiOtherLo);
						// 		}.defer;
						// 		synthStates[i] = 0;
						// 	};
						// }).add,

					OSCFunc(
						{ |msg, time|
							// handles renew actions, play from basic new
							// pause from basic new
							// new and free + new
							([0,6].includes(synthActionStates[i])).if {
								{
									(synthStopModeButtons[i].value != 2).if {
										(synthButtonGroups[i][2]).states_([[
												"", fontColor, stopPlayerButtonColor
											]]).refresh;
									};
									basicNewViews[i].background = liteGreyBackgroundColor;

									this.playerGuiAction(synthViewGroups[i], 0, \thisHiOtherLo);
								}.defer;
								synthStates[i] = 0;
							};
						},
						'/n_go',
						varGui.server.addr,
						argTemplate: [y.asNodeID]
					),

						// OSCpathResponder(varGui.server.addr, ['/n_on', y.asNodeID], { |time, resp, msg|
						// 	((synthActionStates[i] == 1) || parentHelpSynths[i].notNil).if { // play after pause
						// 		{
						// 			basicNewViews[i].background = liteGreyBackgroundColor;
						// 			this.playerGuiAction(synthViewGroups[i], 0, \thisHiOtherLo);
						// 		}.defer;
						// 		synthStates[i] = 0;
						// 	};
						// }).add,

					OSCFunc(
						{ |msg, time|
							((synthActionStates[i] == 1) || parentHelpSynths[i].notNil).if {
								// play after pause
								{
									basicNewViews[i].background = liteGreyBackgroundColor;
									this.playerGuiAction(synthViewGroups[i], 0, \thisHiOtherLo);
								}.defer;
								synthStates[i] = 0;
							};
						},
						'/n_on',
						varGui.server.addr,
						argTemplate: [y.asNodeID]
					),

						// OSCpathResponder(varGui.server.addr, ['/n_off', y.asNodeID], { |time, resp, msg|
						// 	([2,4,7].includes(synthActionStates[i]) || parentHelpSynths[i].notNil).if {
						// 		{
						// 			basicNewViews[i].background = liteGreyBackgroundColor;
						// 			((synthStopModeButtons[i].value != 2) && parentHelpSynths[i].isNil).if {
						// 				(synthButtonGroups[i][2]).states_([["", fontColor, stopPlayerButtonColor]]).refresh;
						// 			};
						// 		}.defer;
						// 		((synthActionStates[i] == 2)  || parentHelpSynths[i].notNil).if { // pause after play
						// 			{ this.playerGuiAction(synthViewGroups[i], 1, \thisHiOtherLo); }.defer;
						// 			synthStates[i] = 1;
						// 		}{	// new paused
						// 			{ this.playerGuiAction(synthViewGroups[i], 1, \thisResetOtherLo); }.defer;
						// 			synthStates[i] = 3;
						// 		};
						// 	};
						// }).add,

					OSCFunc(
						{ |msg, time|
							([2,4,7].includes(synthActionStates[i]) || parentHelpSynths[i].notNil).if {
								{
									basicNewViews[i].background = liteGreyBackgroundColor;
									((synthStopModeButtons[i].value != 2) && parentHelpSynths[i].isNil).if {
										(synthButtonGroups[i][2]).states_([[
												"", fontColor, stopPlayerButtonColor
										]]).refresh;
									};
								}.defer;
								((synthActionStates[i] == 2)  || parentHelpSynths[i].notNil).if {
									// pause after play
									{ this.playerGuiAction(synthViewGroups[i], 1, \thisHiOtherLo); }.defer;
									synthStates[i] = 1;
								}{	// new paused
									{ this.playerGuiAction(synthViewGroups[i], 1, \thisResetOtherLo); }.defer;
									synthStates[i] = 3;
								};
							};
						},
						'/n_off',
						varGui.server.addr,
						argTemplate: [y.asNodeID]
					),

						// OSCpathResponder(varGui.server.addr, ['/n_end', y.asNodeID], { |time, resp, msg|
						//
						// 	{
						// 		([1,2].includes(synthStopModeButtons[i].value)).if {
						// 			this.playerGuiAction(synthViewGroups[i], 2, \thisStopOtherLo);
						// 			(synthButtonGroups[i][2]).states_([["", fontColor, resetPlayerButtonColor]]).refresh;
						// 		}{
						// 			this.playerGuiAction(synthViewGroups[i], 2, \thisHiOtherLo);
						// 			(synthButtonGroups[i][2]).states_([["", fontColor, stopPlayerButtonColor]]).refresh;
						// 		};
						// 		basicNewViews[i].background = liteGreyBackgroundColor;
						//
						// 	}.defer(0.05); // wait if stop by CmdPeriod
						// 	synthStates[i] = 2;
						// }).add

					OSCFunc(
						{ |msg, time|
							{
								([1,2].includes(synthStopModeButtons[i].value)).if {
									this.playerGuiAction(synthViewGroups[i], 2, \thisStopOtherLo);
									(synthButtonGroups[i][2]).states_([[
											"", fontColor, resetPlayerButtonColor
									]]).refresh;
								}{
									this.playerGuiAction(synthViewGroups[i], 2, \thisHiOtherLo);
									(synthButtonGroups[i][2]).states_([[
											"", fontColor, stopPlayerButtonColor
									]]).refresh;
								};
								basicNewViews[i].background = liteGreyBackgroundColor;

							}.defer(0.05); // wait if stop by CmdPeriod
							synthStates[i] = 2;
						},
						'/n_end',
						varGui.server.addr,
						argTemplate: [y.asNodeID]
					)
				]
			};

			makeSynthResponders[i].(x);

			synthButtonGroups[i] = [
				Button(synthViewGroups[i][0], rectIn).states_([["", fontColor, playPlayerButtonColor]])
					.action_({ |b,mod| varGui.server.listSendBundle(latencies[latencyIndex],
						this.dispatchCollect(mouseDownOn.not, mod, synthPlayActions, streamPlayActions, \synth, i)) })
					.mouseDownAction_({ |b,x,y,mod| varGui.server.listSendBundle(latencies[latencyIndex],
						this.dispatchCollect(mouseDownOn, mod, synthPlayActions, streamPlayActions, \synth, i)) }),
				Button(synthViewGroups[i][1], rectIn).states_([["", fontColor, pausePlayerButtonColor]])
					.action_({ |b,mod| varGui.server.listSendBundle(latencies[latencyIndex],
						this.dispatchCollect(mouseDownOn.not, mod, synthPauseActions, streamPauseActions, \synth, i)) })
					.mouseDownAction_({ |b,x,y,mod| varGui.server.listSendBundle(latencies[latencyIndex],
						this.dispatchCollect(mouseDownOn, mod, synthPauseActions, streamPauseActions, \synth, i)) }),
//				Button(synthViewGroups[i][2], rectIn).states_([["", fontColor, stopPlayerButtonColor]])
//					.action_({ |b,mod| varGui.server.listSendBundle(latencies[latencyIndex],
//						this.dispatchCollect(mouseDownOn.not, mod, synthStopActions, streamResetActions, \synth, i)) })
//					.mouseDownAction_({ |b,x,y,mod| varGui.server.listSendBundle(latencies[latencyIndex],
//						this.dispatchCollect(mouseDownOn, mod, synthStopActions, streamResetActions, \synth, i)) }),
				parentHelpSynths[i].isNil.if {
					Button(synthViewGroups[i][2], rectIn).states_([["", fontColor, stopPlayerButtonColor]])
						.action_({ |b,mod| varGui.server.listSendBundle(latencies[latencyIndex],
							this.dispatchCollect(mouseDownOn.not, mod, synthStopActions, streamResetActions, \synth, i, 1, true)) })
						.mouseDownAction_({ |b,x,y,mod| varGui.server.listSendBundle(latencies[latencyIndex],
							this.dispatchCollect(mouseDownOn, mod, synthStopActions, streamResetActions, \synth, i, 1, true)) })
				}{
					Button(synthViewGroups[i][2], rectIn).states_([["", fontColor, liteGreyBackgroundColor]])
						.action_({ |b,mod| }).mouseDownAction_({ |b,x,y,mod| })
				};

			];


			playerView.decorator.shift(x: columnGroupGap);

			synthRenewModeStates[i].if {
				synthRenewModeViews[i] = CompositeView(playerView, modeRectOut);

				synthRenewModeActions[i] = { |b|
					{
						(synthStopModeButtons[i].value != 0).if {
							synthRenewModeViews[i].background = b.value.switch(
								0, { playBackgroundColor },
								1, { pauseBackgroundColor },
								2, { synthBasicNewColor }
							);
						}{	// no change of state while strict stop mode
							synthRenewModeButtons[i].value_(synthRenewModeButtons[i].value + 2 % 3)
						};
					}.defer;
				};

				{
					var v = 0;
					synthRenewModeButtons[i] = parentHelpSynths[i].isNil.if {
						Button(synthRenewModeViews[i], modeRectIn).states_([["", fontColor, modeButtonColor],
							["", fontColor, modeButtonColor], ["", fontColor, modeButtonColor]])
							.action_({|b,mod| this.dispatchPerform(synthRenewModeButtons, modifier, synthRenewModeActions, \synthRenew, \synth, i); })
							// qt small button workaround
							.mouseDownAction_({|b,x,y,m| v = v + 1 % 3; modifier = m; b.valueAction_(v) });
					}{
						Button(synthRenewModeViews[i], modeRectIn).states_([["", fontColor, liteGreyBackgroundColor],
							["", fontColor, liteGreyBackgroundColor], ["", fontColor, liteGreyBackgroundColor]])
					};

					synthRenewModeButtons[i].valueAction_(0);
					parentHelpSynths[i].notNil.if { synthRenewModeViews[i].background = liteGreyBackgroundColor };

				}.();

				{
					var outer = CompositeView(playerView, modeRectOut);
					synthStopModeViews[i] = [outer, CompositeView(outer, modeRectOutTop), CompositeView(outer, modeRectOutBottom)];
				}.();



				synthStopModeActions[i] = { |b, mod|
					var x,y;
					{
						synthStopModeViews[i][1].background =
							b.value.switch(0, { stopBackgroundColor }, 1, { resetBackgroundColor }, 2, { resetBackgroundColor });
						synthStopModeViews[i][2].background =
							b.value.switch(0, { stopBackgroundColor }, 1, { stopBackgroundColor }, 2, { resetBackgroundColor });

						b.value.switch(
							0, {
								(synthButtonGroups[i][2]).states_([["", fontColor, stopPlayerButtonColor]]).refresh;
								(synthStates[i] == 2).if { this.playerGuiAction(synthViewGroups[i], 2, \thisHiOtherLo); };
								synthRenewModeViews[i].background = buttonLoColor;
							},
							1, {
								x = synthRenewModeButtons[i].value; // force update
								[(x + 1) % 3, x].do {|y| synthRenewModeButtons[i].valueAction_(y); };
								(synthStates[i] == 2).if {
									(synthButtonGroups[i][2]).states_([["", fontColor, resetPlayerButtonColor]]).refresh;
									this.playerGuiAction(synthViewGroups[i], 2, \thisStopOtherLo);
								}{	// necessary ?
									(synthButtonGroups[i][2]).states_([["", fontColor, stopPlayerButtonColor]]).refresh;
								}
							},
							2, {
								x = synthRenewModeButtons[i].value;
								[(x + 1) % 3, x].do {|y| synthRenewModeButtons[i].valueAction_(y); };
								(synthButtonGroups[i][2]).states_([["", fontColor, resetPlayerButtonColor]]).refresh;
								(synthStates[i] == 2).if { this.playerGuiAction(synthViewGroups[i], 2, \thisStopOtherLo); };
							}
						)
					}.defer;
				};

				{
					var v = 0;
					synthStopModeButtons[i] = parentHelpSynths[i].isNil.if {
						Button(synthStopModeViews[i][0], modeRectIn).states_([["", fontColor, modeButtonColor],
							["", fontColor, modeButtonColor], ["", fontColor, modeButtonColor]])
							.action_({|b,mod| this.dispatchPerform(synthStopModeButtons, modifier, synthStopModeActions, \synthStop, \synth, i); })
							// qt small button workaround
							.mouseDownAction_({|b,x,y,m| v = v + 1 % 3; modifier = m; b.valueAction_(v) });
					}{
						Button(synthStopModeViews[i][0], modeRectIn).states_([["", fontColor, liteGreyBackgroundColor],
							["", fontColor, liteGreyBackgroundColor], ["", fontColor, liteGreyBackgroundColor]])

					}
				}.();

				synthStopModeButtons[i].valueAction_(1);
				parentHelpSynths[i].notNil.if { [0,1].do { |j| synthStopModeViews[i][j].background = liteGreyBackgroundColor } };

			}{
				playerView.decorator.shift(x: (modeRectOut.width + decoratorGapX) * 2 );
			};

			playerView.decorator.shift(x: columnGroupGap);

			outputs = case
				{ [Synth, String, Symbol].any(synths[i].isKindOf(_)) }
					{
						desc = SynthDescLib.global[synthDefSymbols[i]];
						desc.notNil.if {
							desc.outputs.collect(_.rate).asSet;
						}{
							[]
						};
					}
				{ synths[i].isKindOf(Integer) }{ [] };

			synthRateTexts[i] = StaticText(playerView, smallRectOut).string_(
				case
					{ (outputs.includes(\audio)) && (outputs.includes(\control))  }{  "ar kr" }
					{ outputs.includes(\audio) }{  "ar" }
					{ outputs.includes(\control) }{  "kr" }
					{ true }{  "?" };
				).background_(liteGreyBackgroundColor).stringColor_(fontColor).align_(\center);

			playerView.decorator.nextLine;

			case
				{ synthStates[i] == -1 }{ basicNewViews[i].background = synthBasicNewColor }
				// can only be assumed with newPaused from HS
				{ synthStates[i] == 3 }{ this.playerGuiAction(synthViewGroups[i], 1, \thisResetOtherLo); }
				{ [0,1,2].includes(synthStates[i]) }{ this.playerGuiAction(synthViewGroups[i], synthStates[i], \thisHiOtherLo); };

		  });
		};

		// stream states:

		// -1: not yet playing (basic new)
		// 0: playing, running
		// 1: playing, not running
		// 2: not playing any more
		// 3: pausing, refreshed


		placeStreamPlayers = {
		  varGui.streams.do({|x,i|

			streamEnvirViews[i] = StaticText(playerView, basicNewRect).string_(
				(varGui.streamEnvirIndices[i] >= 10).if { " e" }{ " e " } ++
					(varGui.streamEnvirIndices[i]).asString).background_(liteGreyBackgroundColor).stringColor_(fontColor);

			streamViewGroups[i] = Array.fill(3, { CompositeView(playerView, rectOut) });
			streamViewGroups[i].do(_.background = liteGreyBackgroundColor);

			streamPlayActions[i] = { |b|
				([-1,1,3].includes(streamStates[i])).if {
					varGui.streams[i].play(varGui.clocks[i], quant: varGui.quants[i]);
				}
			};
			streamPauseActions[i] = { |b|
				([-1,0].includes(streamStates[i])).if {
					varGui.streams[i].pause;
				}
			};
			streamResetActions[i] = { |b|

				varGui.streams[i].reset;
				// reset type play

				(streamResetModeButtons[i].value == 0).if {
					[-1,1,2,3].includes(streamStates[i]).if {
						varGui.streams[i].play(varGui.clocks[i], quant: varGui.quants[i]);
					}{
						{ this.playerGuiAction(streamViewGroups[i], 2, \thisHi); }.defer;
						{ this.playerGuiAction(streamViewGroups[i], 2, \thisLo); }.defer(0.1);
					};
				}{  // reset type pause
					[0,1,2].includes(streamStates[i]).if {

						(streamStates[i] != 0).if {
							streamStates[i] = 3;
							{ this.playerGuiAction(streamViewGroups[i], 1, \thisResetOtherLo); }.defer;
						}{
							streamStates[i] = 3;
							varGui.streams[i].pause;
						};
					};
				};
			};

			streamButtonGroups[i] = [
				Button(streamViewGroups[i][0], rectIn).states_([["", fontColor, playPlayerButtonColor]])
					.action_({ |b,mod| varGui.server.listSendBundle(latencies[latencyIndex],
						this.dispatchCollect(mouseDownOn.not, mod, synthPlayActions, streamPlayActions, \stream, i)) })
					.mouseDownAction_({ |b,x,y,mod| varGui.server.listSendBundle(latencies[latencyIndex],
						this.dispatchCollect(mouseDownOn, mod, synthPlayActions, streamPlayActions, \stream, i)) }),
				Button(streamViewGroups[i][1], rectIn).states_([["", fontColor, pausePlayerButtonColor]])
					.action_({ |b,mod| varGui.server.listSendBundle(latencies[latencyIndex],
						this.dispatchCollect(mouseDownOn.not, mod, synthPauseActions, streamPauseActions, \stream, i)) })
					.mouseDownAction_({ |b,x,y,mod| varGui.server.listSendBundle(latencies[latencyIndex],
						this.dispatchCollect(mouseDownOn, mod, synthPauseActions, streamPauseActions, \stream, i)) }),
				Button(streamViewGroups[i][2], rectIn).states_([["", fontColor, resetPlayerButtonColor]])
					.action_({ |b,mod| varGui.server.listSendBundle(latencies[latencyIndex],
						this.dispatchCollect(mouseDownOn.not, mod, synthStopActions, streamResetActions, \stream, i, 1, true)) })
					.mouseDownAction_({ |b,x,y,mod| varGui.server.listSendBundle(latencies[latencyIndex],
						this.dispatchCollect(mouseDownOn, mod, synthStopActions, streamResetActions, \stream, i, 1, true)) }),
			];

			playerView.decorator.shift(x: columnGroupGap);

			streamResetModeViews[i] = CompositeView(playerView, modeRectOut);

			streamResetModeActions[i] = { |b|
				b.value.switch(
					0, { streamResetModeViews[i].background = playBackgroundColor },
					1, { streamResetModeViews[i].background = pauseBackgroundColor }
				);
			};

			{
				var v = 0;
				streamResetModeButtons[i] =
					Button(streamResetModeViews[i], modeRectIn).states_([["", fontColor, modeButtonColor], ["", fontColor, modeButtonColor]])
						.action_({|b| this.dispatchPerform(streamResetModeButtons, modifier, streamResetModeActions, \streamReset, \stream, i);  })
						// qt small button workaround
						.mouseDownAction_({|b,x,y,m| modifier = m; v = 1-v; b.valueAction_(v)  });
			}.();

			[1,0].do {|x| streamResetModeButtons[i].valueAction_(x) };

			playerView.decorator.shift(x: columnGroupGap + modeRectOut.width + decoratorGapX);
			streamTypeTexts[i] = StaticText(playerView, smallRectOut).string_(varGui.streams[i].isKindOf(Task).if { "task" }{ "esp" })
				.background_(liteGreyBackgroundColor).stringColor_(fontColor).align_(\center);

			playerView.decorator.nextLine;

			streamSimpleControllers[i] = SimpleController(x).put(\userPlayed, {|n|
				([-1,1,2,3].includes(streamStates[i])).if {
					{ this.playerGuiAction(streamViewGroups[i], 0, \thisHiOtherLo); }.defer;
					streamStates[i] = 0;
				};
			}).put(\userStopped, {|n|
				([0,3].includes(streamStates[i])).if {
					{
						(streamStates[i] == 3).if {
							this.playerGuiAction(streamViewGroups[i], 1, \thisResetOtherLo);
						}{
							this.playerGuiAction(streamViewGroups[i], 1, \thisHiOtherLo);
						};
					}.defer;
					(streamStates[i] != 3).if { streamStates[i] = 1 }
				};
			}).put(\stopped, {|n|
				varGui.streams[i].streamHasEnded.if {
					{ this.playerGuiAction(streamViewGroups[i], 2, \thisStopOtherLo); }.defer;
					streamStates[i] = 2;
				}{
					(streamStates[i] != 3).if { streamStates[i] = 1 }
				}
			// e.g. ESP started by quant ...
			}).put(\playing, {|n|
				{ this.playerGuiAction(streamViewGroups[i], 0, \thisHiOtherLo); }.defer;
				streamStates[i] = 0;
			});


			streamStates[i] = case	// init EventStreamPlayer and Task states
				{ varGui.streams[i].streamHasEnded }{ { this.playerGuiAction(streamViewGroups[i], 2, \thisStopOtherLo); }.defer; 2 }
				{ varGui.streams[i].miSC_getIsWaiting }
					{ { this.playerGuiAction(streamViewGroups[i], 1, \thisPlayOtherLo); }.defer; 1 }
				{ varGui.streams[i].wasStopped }
					{ { this.playerGuiAction(streamViewGroups[i], 1, \thisHiOtherLo); }.defer; 1 }
				{ true }{ { this.playerGuiAction(streamViewGroups[i], 0, \thisHiOtherLo); }.defer; 0 };

		  });
		};

		makeIconViewGroup = {
			playerView.decorator.shift(x: basicNewRect.width + decoratorGapX);
			iconViewGroup = Array.fill(3, { UserView(playerView, rectOut).background_(liteGreyBackgroundColor) });
			playerView.decorator.shift(x: columnGroupGap);
			modeNameView = StaticText(playerView, modeNameRect).string_(
				((varGui.synthPlayerNum != 0) && (varGui.streamPlayerNum == 0) && (synthRenewModeStates.every(_==false))).if {
					"no modes"  //
				}{
					"modes"
				}).background_(liteGreyBackgroundColor).stringColor_(fontColor).align_(\center);
		};


		(playerPriority == \synth).if {
			placeSynthPlayers.();
			(varGui.synths.size != 0).if {
				1.do { playerView.decorator.nextLine };
				makeIconViewGroup.();
				1.do { playerView.decorator.nextLine };

			};
			(varGui.streams.size != 0).if {
				(varGui.synths.size != 0).if { playerView.decorator.nextLine };
				placeStreamPlayers.();
				(varGui.synths.size == 0).if {
					playerView.decorator.nextLine;
					makeIconViewGroup.();
				};
			};
		}{
			placeStreamPlayers.();
			(varGui.streams.size != 0).if {
				1.do { playerView.decorator.nextLine };
				makeIconViewGroup.();
				1.do { playerView.decorator.nextLine };
			};
			(varGui.synths.size != 0).if {
				(varGui.streams.size != 0).if { playerView.decorator.nextLine };
				placeSynthPlayers.();
				(varGui.streams.size == 0).if {
					playerView.decorator.nextLine;
					makeIconViewGroup.();
				};
			};
		};

		((varGui.streamPlayerNum + varGui.synthPlayerNum) > 0).if { this.drawPlayers };

		// cleanup

		stopByCmdPeriodActions.add({ { varGui.streams.do({|x,i| this.playerGuiAction(streamViewGroups[i], 1, \thisHiOtherLo); }) }.defer; });
		((varGui.synthPlayerNum + varGui.streamPlayerNum) > 0).if {
			stopByCmdPeriodActions.add({
				stopButton.doAction;
			})
		};
		stopByCmdPeriodActions.do(CmdPeriod.add(_));


		playerView.onClose_({
			stopByCmdPeriodActions.do(CmdPeriod.remove(_));
			{
				synthResponders.do({ |x| x.do { |y| y.disable; y.remove } });
				streamSimpleControllers.do(_.remove);
			}.defer(0.2);
		});

		((varGui.streamPlayerNum + varGui.synthPlayerNum) > 0).if { playerView.decorator.shift(y: optionViewsGapTop) };

		//	option section

		latencies = [0.1, varGui.server.latency, nil];
		latencyIndex = 0;

		optionViewMakers = [{
				mouseActionButton = Button(playerView, smallRectOut)
					.states_([["on", fontColor, optionHiColor], ["off", fontColor, optionLoColor]])
					.action_({ |b| mouseDownOn = b.value.switch(0, { true }, 1, { false }) }).value_(1).valueAction_(0);
			},{
				var v = 0;
				envirIndexButton = Button(playerView, smallRectOut)
					.states_([["on", fontColor, optionHiColor], ["off", fontColor, optionLoColor]])
					.action_({ |b| showEnvirIndex = b.value.switch(
						0, { varGui.refreshVarCtrSliderLabels(true); true },
						1, { varGui.refreshVarCtrSliderLabels(false); false }
					) })
					.mouseDownAction_({ |b| v = 1 - v; b.valueAction_(v)  }) // qt small button workaround
					.value_((varGui.envirs.size > 1).if { 1 }{ 0 }).valueAction_((varGui.envirs.size > 1).if { 0 }{ 1 });
			},{
				var v = 1;
				latencyButton = Button(playerView, smallRectOut)
					.states_([["c", fontColor, optionHiColor], ["s", fontColor, optionHiColor], ["nil", fontColor, optionLoColor]])
					.action_({ |b|
						b.value.switch(
							0, { latencyIndex = 0;
								customLatencyBox.background_(optionHiColor);
								serverLatencyBox.background_(optionLoColor); },
							1, { latencyIndex = 1;
								customLatencyBox.background_(optionLoColor);
								serverLatencyBox.background_(optionHiColor); },
							2, { latencyIndex = 2;
								customLatencyBox.background_(optionLoColor);
								serverLatencyBox.background_(optionLoColor); }
						)
					})
					.mouseDownAction_({ |b| v = v + 1 % 3; b.valueAction_(v)  }) // qt small button workaround
					.value_(2);
			},{
				customLatencyBox = NumberBox(playerView, smallRectOut).clipLo_(0.03).clipHi_(2).value_(0.1)
					.background_(optionLoColor).stringColor_(fontColor).normalColor_(fontColor);
				customLatencyBox.scroll_step = 0.01;
				customLatencyBox.action_({|n| latencies[0] = n.value; });
			},{
				serverLatencyBox = NumberBox(playerView, smallRectOut).clipLo_(0.0).clipHi_(2)
					.background_(optionLoColor)
					.stringColor_(fontColor)
					.normalColor_(fontColor)
					.value_(serverLatency ?? { varGui.server.latency });
				serverLatencyBox.scroll_step = 0.01;
				serverLatencyBox.action_({|n| varGui.server.latency = n.value;  latencies[1] = n.value; });
			}];

		5.do {|i|
			var str;
			str = i.switch(
				0, { "  player action by mouse down " },
				1, { "  show envir index in vars' name fields " },
				2, { "  bundle latency (synth players)" },
				3, { "  custom bundle latency" },
				4, { "  server latency (esplayer players)" }
			);

			(((i == 1) && (varGui.varCtrNum != 0)) ||
				((i == 2) && (varGui.synthPlayerNum != 0)) ||
				((i == 3) && (varGui.synthPlayerNum != 0)) ||
				((i == 4) && (varGui.synthPlayerNum + varGui.streamPlayerNum != 0))).if {
				optionViewTexts = optionViewTexts.add(StaticText(playerView, Rect(0, 0, optionViewWidth, fieldHeight))
						.string_(str).background_(liteGreyBackgroundColor).stringColor_(fontColor));
					playerView.decorator.shift(x: columnGroupGap);
					optionViews = optionViews.add(optionViewMakers[i].());
					playerView.decorator.nextLine;
					optionViewNum = optionViewNum + 1;
			};
		};

		totalHeight = totalHeight + optionViewsGapTop + (optionViewNum * fieldHeight) +
			((optionViewNum + 1) * decoratorGapX) + optionViewsGapBottom + globalButtonHeight;


		playerView.decorator.shift(y: optionViewsGapBottom);

		globalButtonWidth = columnGroupGap + modeRectOut.width + decoratorGapX + 20;
		globalButtonGap = (totalWidth - (3 * globalButtonWidth) - (2 * decoratorGapX)) / 2;

		((varGui.synthCtrNum + varGui.varCtrNum) > 0).if {
			updateButton = Button(playerView, globalButtonWidth @ globalButtonHeight);
			updateButton.states = [["update", fontColor, updateButtonColor]];
			updateButton.action = { this.varGui.update };
		}{
			playerView.decorator.shift(globalButtonWidth + decoratorGapX)
		};

		playerView.decorator.shift(globalButtonGap);

		((varGui.synthCtrNum + varGui.varCtrNum) > 0).if {
			saveButton = Button(playerView, globalButtonWidth @ globalButtonHeight);
    			saveButton.states = [["save as", fontColor, saveButtonColor]];
			saveButton.action = {
				Dialog.savePanel({ arg path;
					File(path, "w").write(varGui.saveData.asCompileString).close;
				},{ })
			}
		}{
			playerView.decorator.shift(globalButtonWidth + decoratorGapX)
		};

		playerView.decorator.shift(globalButtonGap);

		((varGui.synthPlayerNum + varGui.streamPlayerNum) > 0).if {
			stopButton = Button(playerView, globalButtonWidth @ globalButtonHeight);
			stopButton.states = [["stop", fontColor, stopButtonColor]];
			stopButton.action = {
				varGui.streams.do {|item| /*item.free;*/ item.stop; };
				varGui.synthIDs.do {|item, i|
					synthActionStates[i] = 3;

					parentHelpSynths[i].isNil.if {
						(synthStates[i] != 2).if {
							(synthStates[i] == -1).if {
								// basic new
								this.updateSynth(i);
								varGui.server.listSendBundle(nil, [synths[i].newMsg, synths[i].miSC_freeMsg]);
							}{
								varGui.server.sendMsg("/n_free", item.asNodeID);
							}
						}
					}{
						// synth derived from HS
						synths[i].isPlaying.if { varGui.server.sendBundle(nil, synths[i].miSC_runMsg(false)) }
					}
				}
			}
		};
		^this
	}

	playerGuiAction {|viewGroup, j, type|
		type.switch(
			\allLo, { 3.do({|x| (viewGroup[x]).background = buttonLoColor}); },
			\thisHiOtherLo, { viewGroup[j].background = buttonHiColor;
				Set[0,1,2].remove(j).do({|x| (viewGroup[x]).background = buttonLoColor}); },
			\thisResetOtherLo, { viewGroup[j].background = resetBackgroundColor;
				Set[0,1,2].remove(j).do({|x| (viewGroup[x]).background = buttonLoColor}); },
			\thisPlayOtherLo, { viewGroup[j].background = playBackgroundColor;
				Set[0,1,2].remove(j).do({|x| (viewGroup[x]).background = buttonLoColor}); },
			\thisHi, { viewGroup[j].background = buttonHiColor; },
			\thisLo, { viewGroup[j].background = buttonLoColor; },
			\thisStopOtherLo, { viewGroup[j].background = stopBackgroundColor;
				Set[0,1,2].remove(j).do({|x| (viewGroup[x]).background = buttonLoColor}); }
		);
		^this
	}


	dispatchPerform { |buttons, mod, actions, mode, reg, i|
		var num, group, isInSameGroup, isOfSameState;
		(mod.isInteger and: { mod.isShift }).if {
			num = actions.size;
			group = ((reg == \synth).if { synthPlayerGroups }{ streamPlayerGroups }).detect(_.includes(i));
			num.collect { |j|
				isInSameGroup = group.includes(i);
				((mod.isAlt && isInSameGroup) || (mod.isAlt.not)).if {
					((mode == \synthRenew) && // exclude cases when renew mode button greyed by stop mode
						((synthStopModeButtons[j].value == 0) ||
							((synthStopModeButtons[j].value != 0) && (synthStopModeButtons[i].value == 0))
						)
					).not.if {
						// no action if synth from HS
						((reg == \stream) or: { parentHelpSynths[j].isNil }).if {
							(j != i).if { buttons[j].value = buttons[i].value; };
							actions[j].(buttons[i], mod);
						}
					}{
						(j == i).if { buttons[j].value = buttons[j].value - 1 % 3; };
					};
				};
			};

		}{
			actions[i].(buttons[i], mod);
		};
		^this
	}

	dispatchCollect { |doIt, mod, synthActions, streamActions, reg, i, flattenNum = 1, stopButtonAction = false|
		var num, group, actions, isInSameGroup, isOfSameState, synthState, comboOffset, firstActions, secondActions,
			synthStopModeState, synthRenewModeState, streamState, streamResetModeState;

		^doIt.if {
			actions = (reg == \synth).if { synthActions }{ streamActions };
			(mod.isInteger and: { mod.isShift }).if {
				(mod.miSC_isPseudoCaps).if {
					comboOffset = (reg == \synth).if {
						(playerPriority == \synth).if { 0 }{ varGui.streamPlayerNum };
					}{
						(playerPriority == \synth).if { varGui.synthPlayerNum }{ 0 }
					};
					(playerPriority == \synth).if {
						firstActions = synthActions;
						secondActions = streamActions;
					}{
						firstActions = streamActions;
						secondActions = synthActions;
					};

					((mod.isAlt).if { comboPlayerGroups.detect(_.includes(i + comboOffset)) } { synthActions.size + streamActions.size })
						.collect { |j|
							// don't stop synth from HS indirectly with shift + caps
							(j >= firstActions.size).if {
								(((playerPriority == \synth) || stopButtonAction.not) or:
									{ parentHelpSynths[j - firstActions.size].isNil }).if {
										secondActions[j - firstActions.size].();
								}
							}{
								(((playerPriority == \stream) || stopButtonAction.not) or: { parentHelpSynths[j].isNil }).if {
									firstActions[j].();
								}
							}
					}.flatten(flattenNum);
				}{
					num = actions.size;
					group = ((reg == \synth).if { synthPlayerGroups }{ streamPlayerGroups }).detect(_.includes(i));

					synthState = synthStates[i];
					synthStopModeState = synthStopModeButtons[i].();
					synthRenewModeState = synthRenewModeButtons[i].();
					streamState = streamStates[i];
					streamResetModeState = streamResetModeButtons[i].();

					num.collect { |j|
						isInSameGroup = group.includes(j);

						isOfSameState = (reg == \synth).if {
							((synthStates[j] == synthState) &&
								(((synthStopModeState == 0) && (synthStopModeButtons[j].() == 0)) ||
									((synthStopModeState == synthStopModeButtons[j].()) &&
										(synthRenewModeState == synthRenewModeButtons[j].()))))
						}{
							((streamStates[j] == streamState) && (streamResetModeButtons[j].() == streamResetModeState))
						};
						// don't stop synth from HS indirectly with shift
						// lookup parentHelpSynths only if reg = \synth
						( ((reg == \stream) || stopButtonAction.not) or: { parentHelpSynths[j].isNil } ).if {
							case
								{ mod.isCtrl && mod.isAlt }
									{ (isInSameGroup && isOfSameState).if { actions[j].(); } }
								{ mod.isCtrl }
									{ isOfSameState.if { actions[j].(); } }
								{ mod.isAlt }
									{ isInSameGroup.if { actions[j].(); } }
								{ true }
									{ actions[j].(); }
						};
					}.flatten(flattenNum);
				}
			}{
				actions[i].();
			};
		};
	}

}


