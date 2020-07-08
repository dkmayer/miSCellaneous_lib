
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

+VarGui {
	
	possibleColumnBreakIndices {|sliderPriority, allowSynthsBreak, allowVarsBreak, 
		allowSynthBreak, allowArrayBreak, allowEnvirBreak, minPartitionSize| 
		var possibleVarBreakIndices = [], possibleSynthBreakIndices = [], possibleBreakIndices, modSynthCtr, modSynthCtrNums,
		indOffset, indices1, indices2, priority,
		k = 0, // iterate controls
		l = 0; // iterate vars
		
		// collection [0] means possible break if there are controls of other type before 
		// []: don't break apart this control type

		priority = case 
			{ sliderPriority.asSymbol == \var }{ \var }
			{ sliderPriority.asSymbol == \synth }{ \synth }
			{ true }{ Error("sliderPriority must be set to var or synth").throw };
		
		modSynthCtr = synthCtr;
		modSynthCtrNums = synthCtrNum;
		
		forBy(0, varCtr.size - 2, 2, {|i|
			varCtr[i+1].every(_.isSequenceableCollection).if {
    				varCtr[i+1].do({|item,j|
   		   			if ((k <= (varCtrNum - minPartitionSize)) &&
   		   					((k == 0) ||
   		   						(allowVarsBreak &&
   		   							(((j == 0) && (k >= minPartitionSize)) ||
 										((j >= minPartitionSize) && allowArrayBreak &&
 											(j <= ((varCtr[i+1].size - minPartitionSize)))
 										)
 									) and:
   		   							{ allowEnvirBreak || (l == 0) or: { varEnvirIndices[l] != varEnvirIndices[l-1] } }
   		   						)
   		   					),{
	   		   					possibleVarBreakIndices = possibleVarBreakIndices.add(k);
   		   					}
   		   				);
    			 	k = k+1;
      			})	
	   	 	}{
   		   		if (((k <= (varCtrNum - minPartitionSize)) &&
   		   				((k == 0) ||
   		   					((k >= minPartitionSize) &&
   		   						allowVarsBreak and:
   		   						{ allowEnvirBreak || (l == 0) or: { varEnvirIndices[l] != varEnvirIndices[l-1] } }
   		   					)
   		   				)),{
	   		   				possibleVarBreakIndices = possibleVarBreakIndices.add(k);
   		   				}
   		   			);
   		   		k = k+1;
			};
			l = l + 1;
    		});
    	
		k = 0;  // iterate controls
		l = 0;  // iterate 
		   		
		forBy(0, synthCtr.size - 2, 2, {|i|
			synthCtr[i+1].every(_.isSequenceableCollection).if {
    				synthCtr[i+1].do({|item,j|
   		   			if ((k <= (synthCtrNum - minPartitionSize)) &&
   		   					((k == 0) ||
   		   						(allowSynthsBreak &&
   		   							(((j == 0) && (k >= minPartitionSize)) ||
 										((j >= minPartitionSize) && allowArrayBreak &&
 											(j <= ((synthCtr[i+1].size - minPartitionSize)))
 										)
 									) and:
   		   							{ allowSynthBreak || (l == 0) or: { synthCtrSynthIndices[l] != synthCtrSynthIndices[l-1] } }
   		   						)
   		   					),{
	   		   					possibleSynthBreakIndices = possibleSynthBreakIndices.add(k);
   		   					}
   		   				);
    			 	k = k+1;
      			})	
	   	 	}{
   		   		if (((k <= (synthCtrNum - minPartitionSize)) &&
   		   				((k == 0) ||
   		   					((k >= minPartitionSize) &&
   		   						allowSynthsBreak and:
   		   						{ allowSynthBreak || (l == 0) or: { synthCtrSynthIndices[l] != synthCtrSynthIndices[l-1] } }
   		   					)
   		   				)),{
	   		   				possibleSynthBreakIndices = possibleSynthBreakIndices.add(k);
   		   				}
   		   			);
   		   		k = k+1;
			};
			l = l + 1;
    		});
    		
		
		indices1 = (priority == \var).if { possibleVarBreakIndices }{ possibleSynthBreakIndices };
		indices2 = (priority == \var).if { possibleSynthBreakIndices }{ possibleSynthBreakIndices };
		
		(priority == \var).if { 
			indices1 = possibleVarBreakIndices;
			indices2 = possibleSynthBreakIndices;
			indOffset = varCtrNum;
		}{
			indices1 = possibleSynthBreakIndices;
			indices2 = possibleVarBreakIndices;
			indOffset = synthCtrNum;
		};	
		
		possibleBreakIndices = case
			{ (indices1.size == 0) && (indices2.size == 0) }{ [] }
			{ (indices1.size == 0) }
				{ ((indices2[0] != 0).if { indices2 }{ indices2.drop(1) }) + indOffset }
			{ true }
				{ ((indices1[0] != 0).if { indices1 }{ indices1.drop(1) }) ++ (indices2 + indOffset) };
		
		^possibleBreakIndices;	
	}
}
