
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

+ String {

	pfuncPbinds { | pBefore, pReplace, pAfter, exclude, excludeGate = true, 
		excludeDur = false, excludeLegato = false, metaKey = \specs, useGlobalSpecs = true
		post = false, trace = false, num = 1 |
		^this.asSymbol.pfuncPbinds(pBefore, pReplace, pAfter, exclude, excludeGate, 
			excludeDur, excludeLegato, metaKey, useGlobalSpecs, post, trace, num)
	}

	sVarGuiSpecs { | ctrBefore, ctrReplace, ctrAfter, exclude,  
		metaKey = \specs, useGlobalSpecs = true, num = 1 |
		^this.asSymbol.sVarGuiSpecs(ctrBefore, ctrReplace, ctrAfter, exclude, 
			metaKey, useGlobalSpecs, num)
	}
	
	pVarGuiSpecs { | ctrBefore, ctrReplace, ctrAfter, durCtr = #[0.05, 3, \exp, 0, 0.2], 
		legatoCtr = #[0.1, 5, \exp, 0, 0.8], exclude, excludeGate = true, 
		metaKey = \specs, useGlobalSpecs = true, num = 1|
		^this.asSymbol.pVarGuiSpecs(ctrBefore, ctrReplace, ctrAfter, durCtr, legatoCtr, 
			exclude, excludeGate, metaKey, useGlobalSpecs, num)
	}

	sVarGui { | ctrBefore, ctrReplace, ctrAfter, exclude, 
		metaKey = \specs, useGlobalSpecs = true, num = 1, server|
		^this.asSymbol.sVarGui(ctrBefore, ctrReplace, ctrAfter, exclude, metaKey, 
			useGlobalSpecs, num, server)
	}

	pVarGui { | ctrBefore, ctrReplace, ctrAfter, 
		durCtr = #[0.05, 3, \exp, 0, 0.2], legatoCtr = #[0.1, 5, \exp, 0, 0.8], 
		pBefore, pReplace, pAfter, exclude, excludeGate = true, clock, quant, 
		metaKey = \specs, useGlobalSpecs = true, post = false, trace = false, num = 1|
		^this.asSymbol.pVarGui(ctrBefore, ctrReplace, ctrAfter, durCtr, legatoCtr, 
			pBefore, pReplace, pAfter, exclude, excludeGate, clock, quant, 
			metaKey, useGlobalSpecs, post, trace, num)
	}

	miSC_defNameAsArray { ^[this] }
	
}

	
