
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


PHSpar : PHelpSynthPar {}

PHelpSynthPar : PHelpSynthParUse {
	var <>startPattern, <>switchDur, <>switchIndex, <>helpSynthArgs, <>switchOn, <>switchOff, <>set, <>hsStartIndices;
	
	*basicNew { |helpSynthPar, switchDur = 10000, switchIndex = 0, helpSynthArgs, pbindArgs, hsIndices, switchOn = false, switchOff = false, set = true, hsStartIndices = \all|
		^super.basicNew(helpSynthPar, pbindArgs, hsIndices).switchDur_(switchDur).switchIndex_(switchIndex).helpSynthArgs_(helpSynthArgs)
			.switchOn_(switchOn).switchOff_(switchOff).set_(set).hsStartIndices_(hsStartIndices);
	}
	
	*new { |helpSynthPar, switchDur = 10000, switchIndex = 0, helpSynthArgs, pbindArgs, hsIndices, switchOn = false, switchOff = false, set = true, hsStartIndices = \all|
		^this.basicNew(helpSynthPar, switchDur, switchIndex, helpSynthArgs, pbindArgs, hsIndices, switchOn, switchOff, set, hsStartIndices)
			.commonInit.addInit;
	}
	
	inputError { |str|
		Error("\nWrong PHSpar input - " ++ str ++ "\n").throw 
	}
	
	addInputCheck { 
		var helpSynthArgsMsg = "helpSynthArgs must be collection of: nils or collections of key / value pairs, \n" ++
				"size must equal the number of help synths. \n" ++
				"keys: control names of the corresponding help synth \n" ++
				"values: control values or patterns/streams which produce them \n";
		
		helpSynth.isKindOf(HelpSynthPar).not.if {
			this.inputError("helpSynthPar must be an instance of HSpar")
		};
		((switchDur.isKindOf(Number) and: { switchDur > 0 }) || switchDur.isKindOf(Pattern) || switchDur.isKindOf(Stream) ).not.if {
			this.inputError("switchDur must be number > 0 or a pattern / stream producing numbers")
		};
		((switchIndex.isKindOf(Number) and: { (switchIndex >= 0) && (switchIndex < helpSynth.size) }) || 
			switchIndex.isKindOf(Pattern) || switchIndex.isKindOf(Stream) ).not.if {
				this.inputError("switchIndex must be a possible HS synth index or a pattern / stream producing such indices")
			};
		(switchOn.isKindOf(Boolean) || switchOn.isKindOf(Pattern) || switchOn.isKindOf(Stream) ).not.if {
			this.inputError("switchOn must be boolean or a pattern / stream producing booleans")
		};
		(switchOff.isKindOf(Boolean) || switchOff.isKindOf(Pattern) || switchOff.isKindOf(Stream) ).not.if {
			this.inputError("switchOff must be boolean or a pattern / stream producing booleans")
		};
		(set.isKindOf(Boolean) || set.isKindOf(Pattern) || set.isKindOf(Stream) ).not.if {
			this.inputError("set must be boolean or a pattern / stream producing booleans")
		};
		hsStartIndices.miSC_isKindOfPossibleHSstartIndices(helpSynth.size).not.if {
			this.inputError("hsStartIndices must be collection of possible help synth indices, \\all or \\none.")
		};
		(helpSynthArgs.isNil || 
			(helpSynthArgs.isKindOf(SequenceableCollection) and: {
				(helpSynthArgs.size == helpSynth.size) && 
					helpSynthArgs.every({|x,i| x.isNil or: { 
						x.isKindOf(SequenceableCollection) and: { 
							x.miSC_isPossibleHelpSynthParArgs(helpSynth.controlNames[i]) 
						} 
					}})
				}
			)
		).not.if {
			this.inputError(helpSynthArgsMsg);
		};
	}

	addInit	{
		hsStartIndices = hsStartIndices.miSC_normalizeHSstartIndices(helpSynth.size);
		startPattern = Pbind(
			\dur, switchDur, 
			\index, switchIndex,
		   	\switchOn, switchOn,
			\switchOff, switchOff, 
		   	\setArgs, set,
			\hsStartIndices, hsStartIndices,
			\theseArgs, Pswitch1(helpSynth.size.collect({|i| 
				(helpSynthArgs.isNil or: { helpSynthArgs[i].size == 0 }).if { [\dummyArg, 0] }{ Pclutch(Ptuple(helpSynthArgs[i]), Pkey(\setArgs)) }}), 
				Pkey(\index)),
			\dummy, Pfunc({|e| e.use {
					var args = [~index, ~theseArgs, ~switchOn, ~switchOff, ~setArgs, ~hsStartIndices] ;  
					helpSynth.currentSwitchIndex = ~index; 
					helpSynth.play(*args);
					};
				}),
			\type, \rest);
	}
	
	play { |clock, quant, hsPlay|
		// dummy arg hsPlay 
		var warnMsg = "HSpar already playing - use " ++
			"PHSparUse for using a playing or pausing HSpar (or stop and free by Cmd-.)\n";
		(helpSynth.isPlaying && helpSynth.hasJustStartedWithPause.not).if { 
			warnMsg.warn 
		}{
			helpSynth.isPHSplaying = true;
			^PHSparPlayer(this).play(clock, quant.asQuant, true);
		}
	}

	// for use with VarGui
	asTask { |clock, quant, hsStop = false, hsPlay = true, switchStop = false, newEnvir = true, removeCtrWithCmdPeriod = true|
		var envir, task, player = this, controller;
		envir = newEnvir.if { () }{ currentEnvironment };
		task = envir.use { Task { 1e5.wait } };
		controller =  SimpleController(task)
			.put(\userPlayed, { envir.use { player = player.play(clock: clock, quant: quant.asQuant, hsPlay: hsPlay) } })
			.put(\userStopped, { player.stop(hsStop, switchStop) });
		removeCtrWithCmdPeriod.if { CmdPeriod.doOnce { controller.remove } };		^task
	}
}

