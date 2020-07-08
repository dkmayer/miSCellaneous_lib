
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
	
	effectiveSliderWidth {|columnNum, maxColumnNum, minSliderWidth, maxSliderWidth| 
		var k,d;
		^[0,1].includes(columnNum).if { 
			maxSliderWidth 
		}{ 
			k = (maxSliderWidth - minSliderWidth)/(1 - maxColumnNum);
			d = maxSliderWidth - k;
			columnNum * k + d;
		}
	}

	placeSliders {|sliderPriority, sliderType, cview, columnBreakIndices, 
		sliderWidth, sliderHeight, labelWidth, numberWidth, 
		synthNumWidth, tempColors, numColorExp, columnIndent, 
		gap, leftMargin, varColorNum, synthColorNum, colorDeviation, greyMode, fontColor|
		var k = 0, m = 0, c = 0, r = 0, rMax = 0, columnBreakCheck, placeVarSliders, 
			placeSynthSliders, afterBreak = false, counterUpdate, sliderClass, 
			tempCtrColors, orderedCtrColorPairs, colorDeviationNums, synthOffset, varOffset, 
			nextLinePlusMaybeLabel, mod = 256;
			
		sliderClass = sliderType.switch(
			\standard, { EZSlider },
			\smooth, { EZSmoothSlider },
			\round, { EZRoundSlider }
		);
		
		// counters:
		// m: sliders
		// k: slider per type
		// c: column
		// r, rMax: max row number, also counts gap between var and synth sliders 

		cview.keyModifiersChangedAction_({ |view, modifiers| mod = modifiers});
		
		colorDeviationNums =  Array.newClear(varColorNum + synthColorNum);
		
		orderedCtrColorPairs = ((varColorNum + synthColorNum) != 0).if {
			(sliderPriority == \var).if {
				varCtrColorPairs ++ ((synthCtrColorPairs.size != 0).if { synthCtrColorPairs + [[varColorNum, 0]] })
			}{
				synthCtrColorPairs ++ ((varCtrColorPairs.size != 0).if { varCtrColorPairs + [[synthColorNum, 0]] }) 
			};
		}{
			[]
		};
		
		orderedCtrColorPairs.do {|x,i|
			(colorDeviationNums[x[0]].isNil or: { colorDeviationNums[x[0]] < x[1] }).if {
				colorDeviationNums[x[0]] = x[1]
			};
		};
		colorDeviationNums = colorDeviationNums + 1;
		
		tempCtrColors = colorDeviationNums.collect {|x,i|
			var c = tempColors[i], p = greyMode.if { (0..x-1).scramble/(x-1) } { x.miSC_distinctCubePoints(3) };
			x.collect({|j| 
				var q = (greyMode.not.if { p[j] }{ p[j]!3 }).linlin(0,1, 1 / (1 + colorDeviation), (1 + colorDeviation));
				Color(c.red * q[0], c.green * q[1], c.blue * q[2]);
			})	
		};
			
		columnBreakCheck = { |i| 
			(r > rMax).if { rMax = r };
			(i == columnBreakIndices[c]).if { 
				cview.decorator.reset;
				c = c + 1;
				r = 0;
				afterBreak = true;
			} 
		};
		
		counterUpdate = {
			k = k+1;
    	  		m = m+1;
    	  		r = r+1;
    	  		afterBreak = false;
  	    		columnBreakCheck.(m);
		};

		nextLinePlusMaybeLabel = {|synthIndex, j, l = 0|
			cview.decorator.nextLine;
			
	 		(hasConnectedSynthCtrGroups.not or: { ((firstSynthCtrIndices.includes(j)  && (l == 0)) || afterBreak) }).if {   		 		cview.decorator.shift((sliderWidth + columnIndent) * c - synthNumWidth - gap);
	 		   	synthNameBoxes = synthNameBoxes.add(
 		   			StaticText(cview, Rect(0, 0, synthNumWidth, sliderHeight)) 
 		   				.background_(tempColors[synthCtrColorPairs[k][0] + synthOffset].miSC_exp(numColorExp))
 		   				.string_( (hasConnectedSynthCtrGroups && afterBreak and: { firstSynthCtrIndices.includes(j).not } ).if { 
	 		   				"(" ++ (synthIndex.asString) ++ ")" 
	 		   			}{ 
		 		   			synthIndex.asString 
		 		   		})
  		   				.align_(\center).stringColor_(fontColor)
 		   		);
  	 		};
  	 		/* avoid shift here */
 		   	cview.decorator.left_((sliderWidth + columnIndent) * c + leftMargin);
		};

		placeVarSliders = {
			forBy(0, varCtr.size - 2, 2, {|i|
				var str, color;
				
				((k == 0) && (m != 0) && afterBreak.not).if { r = r+1; cview.decorator.shift(y: sliderHeight + gap); };
				color = tempCtrColors[varCtrColorPairs[k][0] + varOffset].at(varCtrColorPairs[k][1]);
	    				
    				varCtr[i+1].every(_.isSequenceableCollection).if {
    					varCtr[i+1].do({|item,j|
   			 			str = varCtr[i].asString ++ "[" ++ j.asString ++ "]" ++ " ";
     		 			cview.decorator.nextLine;
  			 			cview.decorator.shift((sliderWidth + columnIndent) * c);
		    				varStrings = varStrings.add(str);
    						varCtrSliders = varCtrSliders.add(
    							sliderClass.new(cview, sliderWidth @ sliderHeight, str, 
    								ControlSpec(*(varCtr[i+1][j].copyFromStart(3))), 
      	  						{|ez| this.performSliderActions(varCtr[i], ez.value, \var, mod, i.div(2), j) }, 
      	  						varCtr[i+1][j].at(4), labelWidth: labelWidth, numberWidth: numberWidth
      	  					).miSC_colorize(color, numColorExp, fontColor)
  	    	  				);
    	  	  				counterUpdate.();
      				})	
	   	 		}{		
    					str = varCtr[i].asString ++ " ";
    					cview.decorator.nextLine;
    					cview.decorator.shift((sliderWidth + columnIndent) * c);
    					varStrings = varStrings.add(str);
	
	    				varCtrSliders = varCtrSliders.add(   						sliderClass.new(cview, sliderWidth @ sliderHeight, str, 
    							ControlSpec(*varCtr[i+1].copyFromStart(3)), 
      	  					{|ez| this.performSliderActions(varCtr[i], ez.value, \var, mod, i.div(2), nil) }, 
      	  					varCtr[i+1].at(4), labelWidth: labelWidth, numberWidth: numberWidth
      	  				).miSC_colorize(color, numColorExp, fontColor)
   		   			);
    	  	  			counterUpdate.();
				};	
    			});
		};

		placeSynthSliders = {
			forBy(0, synthCtr.size - 2, 2, {|j|
				var str, color, synthIndex = synthCtrSynthIndices[j.div(2)];
				
				((k == 0) && (m != 0) && afterBreak.not).if { r = r+1; cview.decorator.shift(y: sliderHeight + gap); };
    				color = tempCtrColors[synthCtrColorPairs[k][0] + synthOffset].at(synthCtrColorPairs[k][1]);

				synthCtr[j+1].every(_.isSequenceableCollection).if {
					synthCtr[j+1].do({|item,l|
						nextLinePlusMaybeLabel.(synthIndex, j.div(2), l);
						str = synthCtr[j].asString ++ "[" ++ l.asString ++ "]" ++ " ";
						synthCtrStrings = synthCtrStrings.add(str);
  		   				synthCtrSliders = synthCtrSliders.add(    							sliderClass.new(cview, sliderWidth @ sliderHeight, str, 
    								ControlSpec(*synthCtr[j+1][l].copyFromStart(3)), 
      	  						{|ez| this.performSliderActions(synthCtr[j], ez.value, \synth, mod, j.div(2), l) },
      	  						 synthCtr[j+1][l].at(4), labelWidth: labelWidth, numberWidth: numberWidth
      	  					).miSC_colorize(color, numColorExp, fontColor)
	      	  			);
	      	  			counterUpdate.();
    					})	
				}{  
					nextLinePlusMaybeLabel.(synthIndex, j.div(2));
	    				str = synthCtr[j].asString ++ " ";
					synthCtrStrings = synthCtrStrings.add(str);
 		   			synthCtrSliders = synthCtrSliders.add(    						sliderClass.new(cview, sliderWidth @ sliderHeight, str, 
    							ControlSpec(*synthCtr[j+1].copyFromStart(3)), 
      	  					{|ez| this.performSliderActions(synthCtr[j], ez.value, \synth, mod, j.div(2), nil) }, 
      	  					synthCtr[j+1].at(4), labelWidth: labelWidth, numberWidth: numberWidth
      	  				).miSC_colorize(color, numColorExp, fontColor)
	      	  		);
    	  	  			counterUpdate.();
 				}
 	   		});	
    		};
	
	(sliderPriority == \var).if {
		synthOffset = varColorNum;
		varOffset = 0;
		placeVarSliders.();
		k = 0;
		placeSynthSliders.()
	}{
		synthOffset = 0;
		varOffset = synthColorNum;
		placeSynthSliders.();
		k = 0;
		placeVarSliders.();
	};
	
	// slider section height
	^rMax * (sliderHeight + gap) + (2 * gap);
	}
	
	refreshVarCtrSliderLabels {|showEnvirIndices = false|
		var sliderCount = 0;
		varEnvirIndices.do {|x,i|
			varCtrSizes[i].do {|j|
				varCtrSliders[sliderCount].set(varStrings[sliderCount] ++ (showEnvirIndices.if { " " ++ x.asString }{ "" }));
				sliderCount = sliderCount + 1;
			}
		};
		^this
	}
}
