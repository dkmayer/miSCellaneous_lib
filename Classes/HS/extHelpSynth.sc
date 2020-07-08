
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

+ HelpSynth {
	newPaused { |pHelpSynth, args, latency|
		var synth = Synth.basicNew(helpSynthString, server).register;
		playingHelpSynths.put(0, synth);
		hasJustStartedWithPause = true;
		bus.isNil.if { bus = Bus.control(server, size) };
		isListening.not.if { 
			this.listen(pHelpSynth.relDemandIndexUserNums.size);
		};
 		server.sendBundle(latency ?? demandLatency, synth.newMsg(server, 
 			[\i_out, bus.index, \out, bus] ++ (args ?? []) ), synth.runMsg(false));
		// CmdPeriod.remove(synth);
 		isPlaying = true;
 		isRunning = false;
 		^playingHelpSynths[0]
	}
}

+ PHelpSynth {
	newPaused { |args, latency| ^helpSynth.newPaused(this, args, latency); }
}


+ HelpSynthPar {
	newPaused { |pHelpSynthPar, args, latency|
		// args = [ args1, args2, ... ]
		var synth;

		hasJustStartedWithPause = true;
		bus.isNil.if { bus = Bus.control(server, size) };
		isListening.not.if { 
			this.listen(pHelpSynthPar.relDemandIndexUserNums.size);
		};
		
		size.do { |i|
			synth = Synth.basicNew(helpSynthString ++ "_" ++ i.asString, server).register;
			playingHelpSynths.put(i, synth);
			// CmdPeriod.remove(synth);
 			server.sendBundle(latency ?? demandLatency, synth.newMsg(server, 
 				[\i_out, bus.index + i, \out, bus.index + i] ++ ((args ?? []).at(i)) ?? []), 
 				synth.runMsg(false)
 			);
 			hsRunnings[i] = false;
		};
 		isPlaying = true;
 		// isRunning = false;
 		^playingHelpSynths;
	}
	
}

+ PHelpSynthPar {
	newPaused { |args, latency| ^helpSynth.newPaused(this, args, latency); }

}
