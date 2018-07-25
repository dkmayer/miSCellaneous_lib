
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

+Set {
	miSC_copy { |copyItems = 0, copySets = 1|
		var obj = (copySets == 1).if { this.shallowCopy }{ this };
		^obj.array_(obj.array.collect(_.miSC_copy(copyItems))) }
}


+Object {

	miSC_getCopyCode {
		^IdentityDictionary[
			(0 -> 0),
			(1 -> 1),
			(2 -> 2),
			(false -> 0),
			(true -> 1),
			(\false -> 0),
			(\true -> 1),
			(\deep -> 2),
		].at(this) ? 0
	}


	miSC_copy { |copyItems = 0|
		^(copyItems == 2).if { this.deepCopy }{ (copyItems == 1).if { this.copy }{ this } }
	}


	miSC_maybeWrapAt { ^this }
}

