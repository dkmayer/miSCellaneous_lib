
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



PHSplayer : PHelpSynthPlayer {}

PHelpSynthPlayer : PHelpSynthUsePlayer {

	*new { arg pHelpSynth;
		^super.new(pHelpSynth);
	}
	
	play { |clock, quant, hsPlay = true, pbindPlay = true, quantBufferTime = 0.2|
		var startQuant, demandQuant, receiveQuant, thisClock = clock ?? TempoClock.default, adaptQuant = true, latestQuant, hsStartFunc;

		(justPlaying.not && justStopping.not).if {
			justPlaying = true;
			#startQuant, demandQuant, receiveQuant = 
				helpSynth.makeAdaptedQuants(thisClock, quant.asQuant, adaptQuant, quantBufferTime);
			latestQuant = [startQuant, demandQuant, receiveQuant].sort({|x,y| x.phase <= y.phase}).last;
			Task({ justPlaying = false; }).play(thisClock, quant: latestQuant);
			
			isPlaying.not.if {
				(hsPlay == true).not.if {
					Error("hsPlay must be true at first call of play - false at resume only").throw;
				};
				helpSynth.hasJustStartedWithPause.not.if { helpSynth.allocBus };
				
				CmdPeriod.add(this);
				helpSynth.isListening.not.if { 
					helpSynth.listen(pHelpSynth.relDemandIndexUserNums.size);
				};
				helpSynth.updateDemandIndexOffsets(pHelpSynth);
				helpSynth.resetInitSeconds;
				
				hsStartFunc = helpSynth.hasJustStartedWithPause.if {
					
					{ helpSynth.schedRun(true, helpSynth.demandLatency) }
				}{
					{ helpSynth.play(pHelpSynth.helpSynthArgs ?? [], helpSynth.demandLatency) }
				};
				
				Task(hsStartFunc).play(clock, quant: startQuant);

				#helpSynthDemandPlayer, helpSynthReceivePlayer = 
					[\demandPattern, \receivePattern].collect({|x| pHelpSynth.perform(x).asEventStreamPlayer});
				helpSynth.demandStreams_indexOffsets.put(helpSynthDemandPlayer.originalStream, helpSynth.lastDemandIndexOffset);
				helpSynth.receiveStreams_indexOffsets.put(helpSynthReceivePlayer.originalStream, helpSynth.lastDemandIndexOffset);

				pbindPlay.if {
					helpSynthDemandPlayer.play(clock, quant: demandQuant);
					helpSynthReceivePlayer.play(clock, quant: receiveQuant);
					isPbindPlaying = true;
				};
			
				helpSynth.mainPHSplayer = this;
				// players are freed separately with CmdPeriod (free)
				// [helpSynthDemandPlayer, helpSynthReceivePlayer].do({|x| CmdPeriod.remove(x)}); 
				helpSynth.isPHSplaying = true;
				isPlaying = true;
				
				helpSynth.hasJustStartedWithPause.if {  helpSynth.hasJustStartedWithPause = false };
			}{
				hsPlay.if { Task({ helpSynth.schedRun(true, helpSynth.demandLatency) }).play(clock, quant: startQuant); };
				(pbindPlay && isPbindPlaying.not).if {
					helpSynthDemandPlayer.play(clock, quant: demandQuant);
					helpSynthReceivePlayer.play(clock, quant: receiveQuant);
					isPbindPlaying = true;
				}
			}
		}
	}

	stop { |hsStop = false, pbindStop = true, addAction|
		var clock = helpSynthReceivePlayer.clock, hsStopLatency, 
			nextDemandAbs, stopAbs, stopBufferTime = 0.1;

		(justPlaying.not && justStopping.not).if {
			justStopping = true;
			stopAbs = stopBufferTime * clock.tempo + clock.beats;
			hsStopLatency = helpSynth.demandLatency * clock.tempo;			
			clock.schedAbs(stopAbs, {
				(isPbindPlaying && pbindStop).if {
					nextDemandAbs = helpSynthDemandPlayer.nextBeat;
					hsStop.if {
						helpSynth.schedRun(false, nextDemandAbs - clock.beats / clock.tempo + hsStopLatency); 
					};
					helpSynthDemandPlayer.stop; 
					clock.schedAbs(helpSynth.totalLatency * clock.tempo + clock.beats, { helpSynthReceivePlayer.stop; isPbindPlaying = false; });
					clock.schedAbs((helpSynth.totalLatency + cleanupDelay) * clock.tempo + clock.beats,  { addAction.value; nil; });
					clock.schedAbs((helpSynth.totalLatency + cleanupDelay) * clock.tempo + nextDemandAbs, { justStopping = false; });   
				}{
					clock.schedAbs(cleanupDelay * clock.tempo + stopAbs, { justStopping = false; });   
					hsStop.if { helpSynth.schedRun(false, hsStopLatency); };
				};
				nil;
			})
		}
	}
		
	pause { |hsStop = true, pbindStop = true, addAction|
		this.stop(hsStop, pbindStop, addAction);
	}
	
	cmdPeriod { this.free }
	
	free {
		isPlaying.not.if {
			// "Player not playing".warn;
		}{
			helpSynthDemandPlayer.stop;
			helpSynthReceivePlayer.stop;
			helpSynth.isPHelpSynthPlaying = false; 
			isPlaying = false;
			isPbindPlaying = false;

			helpSynth.otherPHSusePlayers.do(_.free);
			SystemClock.sched(helpSynth.totalLatency + cleanupDelay, { this.clearBookkeeping; });
			helpSynth.free(helpSynth.totalLatency + cleanupDelay);
		};
		CmdPeriod.remove(this);
	}
}

