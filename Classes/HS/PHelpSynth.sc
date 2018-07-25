
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

PHS : PHelpSynth {}

PHelpSynth : PHelpSynthUse { 
	var <>helpSynthArgs;

	*basicNew { |helpSynth, helpSynthArgs ... pbindArgs|
		^super.basicNew(helpSynth, *pbindArgs).helpSynthArgs_(helpSynthArgs);
	}
	
	*new { |helpSynth, helpSynthArgs ... pbindArgs|
		^this.basicNew(helpSynth, helpSynthArgs, *pbindArgs).commonInit;
	}

	inputError { |str|
		Error("\nWrong PHS input - " ++ str ++ "\n").throw 
	}

	addInputCheck { 
		helpSynth.isKindOf(HelpSynthPar).if {
			this.inputError("helpSynth must be an instance of HS")
		};
		(helpSynthArgs.isNil || (helpSynthArgs.isKindOf(SequenceableCollection) and: { 
			helpSynthArgs.miSC_isPossibleHelpSynthArgs(helpSynth.controlNames) })).not.if {
				this.inputError("helpSynthArgs must be collection of key / value pairs, \n" ++
					"keys: control names of defined help synth \n" ++
					"values: control values or functions which produce them \n")
		};		
	}

	play { |clock, quant, hsPlay|
		// dummy arg hsPlay 
		var warnMsg = "HS already playing - use " ++
			"PHSuse for using a playing or pausing HS (or stop and free by Cmd-.)\n";
		(helpSynth.isPlaying && helpSynth.hasJustStartedWithPause.not).if { 
			warnMsg.warn 
		}{
			helpSynth.isPHSplaying = true;
			^PHelpSynthPlayer(this).play(clock, quant.asQuant, true);
		}
	}
	
	// for use with VarGui
	asTask { |clock, quant, hsStop = false, hsPlay = true, newEnvir = true, removeCtrWithCmdPeriod = true|
		var envir, task, player = this, controller;
		envir = newEnvir.if { () }{ currentEnvironment };
		task = envir.use { Task { 1e5.wait } };
		controller =  SimpleController(task)
			.put(\userPlayed, { envir.use { player = player.play(clock: clock, quant: quant.asQuant, hsPlay: hsPlay) } })
			.put(\userStopped, { player.stop(hsStop) });
		removeCtrWithCmdPeriod.if { CmdPeriod.doOnce { controller.remove } };		^task
	}

}
