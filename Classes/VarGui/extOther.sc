
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


// nonprivate, no prefix "miSC_"

+SequenceableCollection {

	// actually could be named pairsDup
	
	specPairsDup { |num| 
		^this.clump(2).collect(_!num).flatten(2) 
	}

	specPairsDupGroups { |num| 
		^(0..(this.size.div(2) * num - 1)).clump(num).flop 
	}	
}


+Event {
	asESP { }
}

+EventStreamPlayer {
	asESP { ^this }
}

+Stream { 
	asESP { |protoEvent| ^this.asEventStreamPlayer(protoEvent) }
}

+Pattern {
	asESP { |protoEvent| ^this.asEventStreamPlayer(protoEvent) }
}

