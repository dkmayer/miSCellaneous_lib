
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


+Color {
	
	miSC_exp { |x| 
		var a = [this.red, this.green, this.blue] ** x ++ [this.alpha];
		^Color(*a);
	}
	
	miSC_iplToGrey { |greyCenter, deviation| 
		// greyCenter >= 0 and <= 1, deviation > 0 and <= 1 
		var a = deviation.linlin(0, 1, greyCenter!3, [this.red, this.green, this.blue]) ++ [this.alpha];
		^Color(*a);
	}
	
	miSC_dim { |ctrButtonGreyCenter, ctrButtonGreyDev| 
		^this.miSC_iplToGrey(ctrButtonGreyCenter, ctrButtonGreyDev)
	}
	
}

	
