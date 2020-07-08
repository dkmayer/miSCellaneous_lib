
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


PHSparPlayer : PHelpSynthParPlayer {}

PHelpSynthParPlayer : PHelpSynthUsePlayer {
	var <>helpSynthPlayer, <>isSwitchPlaying = false; 
	
	*new { arg pHelpSynthPar;
		^super.new(pHelpSynthPar);
	}

	play { |clock, quant, hsPlay = true, switchPlay = true, pbindPlay = true, quantBufferTime = 0.2|
		var startQuant, demandQuant, receiveQuant, latestQuant, thisClock = clock ?? TempoClock.default, hsPlayIndices, adaptQuant = true;

		(justPlaying.not && justStopping.not).if {
			justPlaying = true;
			#startQuant, demandQuant, receiveQuant = 
				helpSynth.makeAdaptedQuants(thisClock, quant.asQuant, adaptQuant, quantBufferTime, switchResumeDelay );
			latestQuant = [startQuant, demandQuant, receiveQuant].sort({|x,y| x.phase <= y.phase}).last;
			Task({ justPlaying = false; }).play(thisClock, quant: latestQuant);
			
			isPlaying.not.if {
				((hsPlay == true) && (switchPlay == true)).not.if {
					Error("hsPlay and switchPlay must be true at first call of play - other values at resume only").throw;
				};
				// helpSynth.allocBus;
				helpSynth.hasJustStartedWithPause.not.if { helpSynth.allocBus };
				
				CmdPeriod.add(this);
				helpSynth.isListening.not.if { 
					helpSynth.listen(pHelpSynth.relDemandIndexUserNums.size);
				};
				helpSynth.updateDemandIndexOffsets(pHelpSynth);
				helpSynth.resetInitSeconds;
			
				#helpSynthPlayer, helpSynthDemandPlayer, helpSynthReceivePlayer = 
					[\startPattern, \demandPattern, \receivePattern].collect({|x| pHelpSynth.perform(x).asEventStreamPlayer});
				helpSynth.demandStreams_indexOffsets.put(helpSynthDemandPlayer.originalStream, helpSynth.lastDemandIndexOffset);
				helpSynth.receiveStreams_indexOffsets.put(helpSynthReceivePlayer.originalStream, helpSynth.lastDemandIndexOffset);
	
				helpSynthPlayer.play(clock, quant: startQuant);
				isSwitchPlaying = true;
								
				pbindPlay.if {
					helpSynthDemandPlayer.play(clock, quant: demandQuant);
					helpSynthReceivePlayer.play(clock, quant: receiveQuant);
					isPbindPlaying = true;
				};
			
				helpSynth.mainPHSplayer = this;
				// players are stopped separately with CmdPeriod (free)
				[helpSynthPlayer, helpSynthDemandPlayer, helpSynthReceivePlayer].do({|x| CmdPeriod.remove(x)}); 
				helpSynth.isPHSplaying = true;
				isPlaying = true;

				helpSynth.hasJustStartedWithPause.if {  helpSynth.hasJustStartedWithPause = false };
			}{
				hsPlayIndices = this.hsInputIndices(hsPlay);
				helpSynth.schedRun(true, helpSynth.demandLatency + quantBufferTime, hsPlayIndices);
			
				(switchPlay && isSwitchPlaying.not).if { helpSynthPlayer.play(clock, quant: startQuant); isSwitchPlaying = true; };
				switchResumeDelay = 0;
			
				(pbindPlay && isPbindPlaying.not).if {
					helpSynthDemandPlayer.play(clock, quant: demandQuant);
					helpSynthReceivePlayer.play(clock, quant: receiveQuant);
					isPbindPlaying = true;
				};
			}
		}
	}

	hsInputIndices { |hsInput|
		var size = helpSynth.size;
		case 
			{ hsInput == \switch } { ^[helpSynth.currentSwitchIndex] }
			{ (hsInput == \none) ||  (hsInput == false) } { ^[] }
			{ (hsInput == \all) ||  (hsInput == true) } { ^(0..size-1) }
			{ hsInput.isInteger and: { (hsInput < size) && (hsInput >= 0) }} { ^[hsInput] }
			{ hsInput.isKindOf(Collection) and: 
				{ hsInput.every({|x| x.isInteger and: { (x < size) && (x >= 0) } }) }} { ^hsInput } 
			{ true } { Error.throw("Wrong hsStop / hsPlay input - must be one of: \\all, \\none, \\switch, true, " ++
				"false, a valid HSpar index (integer) or a collection of valid HSpar indices") }
	}
	
	stop { |hsStop = false, switchStop = false, pbindStop = true, addAction|
		var clock = helpSynthReceivePlayer.clock, someHSstop,  
			hsStopIndices, nextDemandAbs, stopAbs, stopBufferTime = 0.15;
		
		(justPlaying.not && justStopping.not).if {
			justStopping = true;
			hsStopIndices = this.hsInputIndices(hsStop);
			someHSstop = (hsStop != []).if { true }{ false };
		
			// stopAbs: basic stopping delay, 8 cases of stopping ...
			stopAbs = stopBufferTime * clock.tempo + clock.beats;

			clock.schedAbs(stopAbs, {
				(isPbindPlaying && pbindStop).if {
					// now look at next demand from delayed stopping point
					nextDemandAbs = helpSynthDemandPlayer.nextBeat;
					clock.schedAbs(nextDemandAbs, {
						(isSwitchPlaying && switchStop).if { 
							// next switch resume will be delayed by time to next switch from this demand time
							switchResumeDelay = (helpSynthPlayer.nextBeat - clock.beats) / clock.tempo; 
							helpSynthPlayer.stop;
							isSwitchPlaying = false; 
						}{
							switchResumeDelay = 0;
						};
						clock.schedAbs((cleanupDelay + max(helpSynth.totalLatency, switchResumeDelay)) * clock.tempo + nextDemandAbs, {
							justStopping = false;
						});   
						someHSstop.if { helpSynth.schedRun(false, helpSynth.demandLatency, hsStopIndices); };
						nil;
					});
					helpSynthDemandPlayer.stop; 
					clock.schedAbs(helpSynth.totalLatency * clock.tempo + stopAbs, { helpSynthReceivePlayer.stop; isPbindPlaying = false; });
					clock.schedAbs((helpSynth.totalLatency + cleanupDelay) * clock.tempo + stopAbs,  { addAction.value; nil; });
				}{
					(isSwitchPlaying && switchStop).if { 
						switchResumeDelay = (helpSynthPlayer.nextBeat - clock.beats) / clock.tempo; 
						helpSynthPlayer.stop; 
						isSwitchPlaying = false; 
					}{
						switchResumeDelay = 0;
					};
					clock.schedAbs((cleanupDelay + switchResumeDelay) * clock.tempo + stopAbs, { justStopping = false; });   
					someHSstop.if { helpSynth.schedRun(false, helpSynth.demandLatency, hsStopIndices); };
				};
			nil;
			});
		};	
	}
		
	pause { |hsStop = true, switchStop = false, pbindStop = true, addAction|
		this.stop(hsStop, switchStop, pbindStop, addAction);
	}
	
	cmdPeriod { this.free }
	
	free {
		isPlaying.not.if {
			// "Player not playing".warn;
		}{
			helpSynthPlayer.stop;
			helpSynthDemandPlayer.stop;
			helpSynthReceivePlayer.stop;
			helpSynth.isPHelpSynthPlaying = false; 
			isPlaying = false;
			isPbindPlaying = false;
			isSwitchPlaying = false;

			helpSynth.otherPHSusePlayers.do(_.free);
			SystemClock.sched(helpSynth.totalLatency + cleanupDelay, { this.clearBookkeeping; });
			helpSynth.free(helpSynth.totalLatency + cleanupDelay)
		};
		CmdPeriod.remove(this);
	}
}


