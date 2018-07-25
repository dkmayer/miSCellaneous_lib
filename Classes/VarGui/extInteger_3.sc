
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


+Integer {
	miSC_isPseudoCaps { ^this.isCmd }

	miSC_checkColorGroups { |colorGrouping|
		var flattenedColl, bool = false;
		colorGrouping.isSequenceableCollection.not.if {
			SimpleInputError("varColorGroups / synthColorGroups must be SequenceableCollection").throw
		};
		colorGrouping.every { |x| x.isSequenceableCollection.not }.if {
			SimpleInputError("varColorGroups / synthColorGroups must be grouping").throw
		};

		flattenedColl = colorGrouping.flatten(1);
		flattenedColl.any { |x| x.isSequenceableCollection.not }.if {
			(flattenedColl.every { |x| x.isInteger and: { x < this } } and:
				{ flattenedColl.asSet.size == this }).if {
					bool = true;
			};
		};
		bool.not.if {
			SimpleInitError("varColorGroups / synthColorGroups must be correct grouping of number " ++
				this.asString ++ ". E.g. [[2], [0,3], [1,4]] is a valid grouping of 5. You can use method " ++
				"clumps, e.g. (0..4).clumps([1,3,2]).").throw
		};
	}
}
