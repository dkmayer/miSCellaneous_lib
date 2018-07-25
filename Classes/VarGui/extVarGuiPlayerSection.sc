
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

+VarGuiPlayerSection {
	
	drawPlayers {

		iconViewGroup[0].drawFunc = {
			var playX1 = 0.38, playY1 = 0.3, fac = 1,
				x = iconViewGroup[0].bounds.width, y = iconViewGroup[0].bounds.height;
			Pen.moveTo(Point(x * playX1, y * playY1));
			Pen.lineTo(Point(x * (1-playX1), y * 0.5));
			Pen.lineTo(Point(x * playX1, (1-playY1) * y));
			Pen.width = 1.4 * fac;
			Pen.strokeColor = Color.black;
			Pen.stroke;
		};
		
		iconViewGroup[1].drawFunc = {
			var playX2 = 0.43, playY2 = 0.2, fac = 1,
				x = iconViewGroup[1].bounds.width, y = iconViewGroup[1].bounds.height;
			Pen.moveTo(Point(x * playX2, y * playY2));
			Pen.lineTo(Point(x * playX2, y * (1-playY2)));
			Pen.moveTo(Point(x * (1-playX2), y * playY2));
			Pen.lineTo(Point(x * (1-playX2), y * (1-playY2)));
			Pen.width = 1.7 * fac;
			Pen.strokeColor = Color.black;
			Pen.stroke;
		};
		
		iconViewGroup[2].drawFunc = {
			var playX3a = 0.16, playX3a2 = 0.32, playY3a = 0.3, playX3b = 0.45, playY3b = 0.9, playX3c = 0.64,  playY3c = 0.3, fac = 1,
				x = iconViewGroup[2].bounds.width, y = iconViewGroup[2].bounds.height;
				
			Pen.moveTo(Point(x * playX3a, y * playY3a));
			Pen.lineTo(Point(x * playX3a, y * (1-playY3a)));
			Pen.lineTo(Point(x * playX3a2, y * (1-playY3a)));
			Pen.lineTo(Point(x * playX3a2, y * playY3a));
			Pen.lineTo(Point(x * playX3a  - (1 * fac), y * playY3a));
		
			Pen.width = 1.4 * fac;
			Pen.strokeColor = Color.black;
			Pen.stroke;

			Pen.moveTo(Point(x * playX3b, y * playY3b));
			Pen.lineTo(Point(x * (1-playX3b), y * (1-playY3b)));
	
			Pen.width = 1 * fac;
			Pen.strokeColor = Color.black;
			Pen.stroke;

			Pen.moveTo(Point(x * (1-playX3a), y * playY3c));
			Pen.lineTo(Point(x * playX3c, y * 0.5));
			Pen.lineTo(Point(x * (1-playX3a), y * (1-playY3c)));

			Pen.width = 1.2 * fac;
			Pen.strokeColor = Color.black;
			Pen.stroke;
		};
		^this
	}
	
}
