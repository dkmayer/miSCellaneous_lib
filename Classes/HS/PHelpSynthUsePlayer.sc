
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

PHSusePlayer : PHelpSynthUsePlayer {}

PHelpSynthUsePlayer {
	var <>pHelpSynth, <>helpSynth, <>helpSynthDemandPlayer, <>helpSynthReceivePlayer, <>trigMsgIDqueue,
		<>isPlaying, <>isPbindPlaying, <>demandIndexOffset, <>switchResumeDelay = 0, <>cleanupDelay = 0.01,
		<>justPlaying = false, <>justStopping = false;

	*new { arg pHelpSynthUse;
		^super.new.pHelpSynth_(pHelpSynthUse).helpSynth_(pHelpSynthUse.helpSynth)
			.trigMsgIDqueue_(PriorityQueue.new)
			.isPlaying_(false).isPbindPlaying_(false);
		// isPlaying: player already started, also true when pbinds pausing - isPbindPlaying: pbinds are playing
	}
	
	play { |clock, quant, quantBufferTime = 0.2|
		var startQuant, demandQuant, receiveQuant, thisClock = clock ?? TempoClock.default, adaptQuant = true, latestQuant;
		
		(justPlaying.not && justStopping.not).if {
			justPlaying = true;
			#startQuant, demandQuant, receiveQuant = 
				helpSynth.makeAdaptedQuants(thisClock, quant.asQuant, adaptQuant, quantBufferTime);
			latestQuant = [demandQuant, receiveQuant].sort({|x,y| x.phase <= y.phase}).last;
			Task({ justPlaying = false; }).play(thisClock, quant: latestQuant);
			
			isPlaying.not.if {
				CmdPeriod.add(this);
				helpSynth.listen(pHelpSynth.relDemandIndexUserNums.size);
				helpSynth.updateDemandIndexOffsets(pHelpSynth);
			
				helpSynthDemandPlayer = pHelpSynth.demandPattern.play(clock, quant: demandQuant);
				helpSynthReceivePlayer = pHelpSynth.receivePattern.play(clock, quant: receiveQuant);
				helpSynth.demandStreams_indexOffsets.put(helpSynthDemandPlayer.originalStream, helpSynth.lastDemandIndexOffset);
				helpSynth.receiveStreams_indexOffsets.put(helpSynthReceivePlayer.originalStream, helpSynth.lastDemandIndexOffset);

				// [helpSynthDemandPlayer, helpSynthReceivePlayer].do({|x| CmdPeriod.remove(x)}); 

				helpSynth.otherPHSusePlayers.add(this);
				isPlaying = true;
				isPbindPlaying = true;
			}{
				isPbindPlaying.not.if {
					helpSynthDemandPlayer.play(clock, quant: demandQuant);
					helpSynthReceivePlayer.play(clock, quant: receiveQuant);
					isPbindPlaying = true;
				}
			};
		}
	}		
	
	clearBookkeeping {
		trigMsgIDqueue.clear; 
	}
				
	stop { |addAction|
		var clock = helpSynthReceivePlayer.clock, hsStopLatency, stopAbs, stopBufferTime = 0.1;

		(justPlaying.not && justStopping.not).if {
			stopAbs = stopBufferTime * clock.tempo + clock.beats;
			isPbindPlaying.if {
				justStopping = true;
				clock.schedAbs(stopAbs, {
					helpSynthDemandPlayer.stop; 
					clock.schedAbs(helpSynth.totalLatency * clock.tempo + clock.beats, { helpSynthReceivePlayer.stop; isPbindPlaying = false; });
					clock.schedAbs((helpSynth.totalLatency + cleanupDelay) * clock.tempo + clock.beats,  { addAction.value; justStopping = false; });
				});
			}	
		}
	}
	
	pause { |addAction|
		this.stop(addAction);
	}
	
	cmdPeriod { this.free }
	
	free {
		isPlaying.not.if {
			// "Player not playing".warn;
		}{
			helpSynthDemandPlayer.stop;
			helpSynthReceivePlayer.stop;
			isPlaying = false;
			isPbindPlaying = false;
			
			SystemClock.sched(helpSynth.totalLatency + cleanupDelay, { this.clearBookkeeping; });
		};
		CmdPeriod.remove(this);
	}

}
