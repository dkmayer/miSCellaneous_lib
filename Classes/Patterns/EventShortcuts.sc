
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

EventShortcuts {
	classvar <state;	// \on or \off
	classvar dicts; 	// an IdentityDictionary of IdentityDictionaries,
						// named collections of shortcuts
	classvar <current; 	// name of currently prior shortcut dictionary,
						// replacement only if state == \on !
						// current persists with EventShortcuts set off and on again
	classvar orderFunc;
	classvar eventTypes;// currently checked event type functions that have been
						// prefixed by method prefixEventTypes with a Function
						// that does all replacement.
						// With this method (also invoked with EventShortcuts.on)
						// prefixing is done if it hasn't been done before
						// or if event type functions have changed
						// (i.e. event types have been redefined)

	*initClass {
		dicts = IdentityDictionary();
		eventTypes = IdentityDictionary();
		dicts[\default] = (
			a: \amp,
			ct: \ctranspose,
			d: \dur,
			de: \degree,
			dt: \detune,
			f: \freq,
			gt: \gtranspose,
			h: \harmonic,
			i: \instrument,
			l: \legato,
			lat: \latency,
			m: \midinote,
			mt: \mtranspose,
			n: \note,
			o: \octave,
			or: \octaveRatio,
			r: \root,
			p: \pan,
			s: \strum,
			sc: \scale,
			st: \stepsPerOctave,
			str: \stretch,
			su: \sustain,
			t: \tuning,
			to: \timingOffset,
			tr: \trigger,
			v: \velocity
		);
		current = \default;
		state = \off;

		orderFunc = { |x, y|
			var i = 0, result = true, minSize = min(x.size, y.size);
			while { i <= (minSize - 1) }{
				(x[i] < y[i]).if {
					i = minSize;
				}{
					(y[i] < x[i]).if {
						i = minSize;
						result = false
					}{
						i = i + 1;
					}
				}
			};
			result
		}
	}

	*add { |name, dict, overwrite = false|
		[Symbol, String].includes(name.class).not.if {
			Error("name of shortcut dictionary must be Symbol or String").throw
		};
		name = name.asSymbol;
		dict.isKindOf(IdentityDictionary).not.if {
			Error("dict must be IdentityDictionary").throw
		};
		(dict.values ++ dict.keys).every(_.isKindOf(Symbol)).not.if {
			Error("keys and values of shortcut dictionary must be Symbols").throw
		};
		(name == \default).if {
			Error("default dictionary must not be overwritten").throw
		};

		dicts[name].notNil.if {
			overwrite.not.if {
				Error("shortcut dictionary of that name exists " ++
					"for overwriting set overwrite flag of " ++
					"this method to true").throw
			}{
				dicts.put(name.asSymbol, dict);
				("shortcut dictionary of name " ++ name.asSymbol ++ "has been overwritten !").warn
			}
		}{
			dicts.put(name.asSymbol, dict);
		}
	}

	*addOnBase { |baseName = \default, newName, dict, overwrite = false|
		var newDict;

		[Symbol, String].includes(baseName.class).not.if {
			Error("baseName of shortcut dictionary must be Symbol or String").throw
		};
		baseName = baseName.asSymbol;

		[Symbol, String].includes(newName.class).not.if {
			Error("newName of shortcut dictionary must be Symbol or String").throw
		};
		newName = newName.asSymbol;

		dicts[baseName].isNil.if {
			Error("no dictionary of that baseName").throw
		};

		dict.isKindOf(IdentityDictionary).not.if {
			Error("dict must be IdentityDictionary").throw
		};
		(dict.values ++ dict.keys).every(_.isKindOf(Symbol)).not.if {
			Error("keys and values of shortcut dictionary must be Symbols").throw
		};
		(newName == \default).if {
			Error("default dicts must not be overwritten").throw
		};

		newDict = dicts[baseName].copy.putAll(dict);


		dicts[newName].notNil.if {
			overwrite.not.if {
				Error(("shortcut dictionary of that newName exists " ++
					"for overwriting set overwrite flag of " ++
					"this method to true")).throw
			}{
				dicts.put(newName.asSymbol, newDict);
				("shortcut dictionary of name " ++ newName.asString ++ " has been overwritten !").warn
			}
		}{
			dicts.put(newName.asSymbol, newDict);
		}
	}


	*remove { |name|
		[Symbol, String].includes(name.class).not.if {
			Error("name must be Symbol or String").throw
		}{
			name = name.asSymbol
		};

		dicts[name].isNil.if {
			"no dicts dictionary of that name".warn
		}{
			(name == \default).if {
				Error("default dicts must not be removed").throw
			}{
				dicts.put(name, nil)
			}
		}
	}
	*removeAll { (this.dictNames - [\default]).do(this.remove(_)) }

	*copyDict { |name| ^dicts[name].copy }
	*copyCurrentDict { |name| ^dicts[current].copy }
	*copyAllDicts { ^dicts.deepCopy }

	*dictNames { ^dicts.keys }

	*post { |name|
		var orderedKeys = dicts[name].keys.collect(_.asString).asArray.ascii.sort(orderFunc);
		orderedKeys = orderedKeys.collect { |x| x.collect(_.asAscii).join.asSymbol };
		"".postln;
		("Shortcut dictionary of name \\" ++ name.asString ++ ":").postln;
		"".postln;
		orderedKeys.do { |k| k.post; ": ".post; (dicts[name][k]).postln };
		^""
	}

	*postCurrent { this.post(current);^"" }

	*postAll {
		var orderedDictNames = dicts.keys.collect(_.asString).asArray.ascii.sort(orderFunc);
		orderedDictNames = orderedDictNames.collect { |x| x.collect(_.asAscii).join.asSymbol };
		"".postln;
		("All shortcut dictionaries:").postln;
		orderedDictNames.do { |k| this.post(k) };
		^""
	}

	*makeCurrent { |name|
		[Symbol, String].includes(name.class).not.if {
			Error("name of shortcut dictionary must be Symbol or String").throw
		};
		name = name.asSymbol;
		dicts[name].isNil.if {
			Error("no dictionary of that name").throw
		};
		current = name
	}

	*on {
		this.prefixEventTypes;
		state = \on;
		"".postln;
		"EventShortcuts on".postln;
		^""
	}

	*off {
		state = \off;
		"".postln;
		"EventShortcuts off".postln;
		^""
	}

	*miSC_replaceShortcuts { |e|
		(EventShortcuts.state == \on).if { e.miSC_replaceKeys(dicts[current]) };
		^e
	}

	*prefixEventTypes {
		var prefixFunc;
		Event.default.eventTypes.keysValuesDo { |k,v|
			(eventTypes[k] === v).not.if {
				eventTypes.put(k, v);
				prefixFunc = { |server|
					currentEnvironment.eventShortcuts;
					server
				};
				Event.addEventType(k, Event.default.eventTypes[k] <> prefixFunc);
			}
		}
	}

}




