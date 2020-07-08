
======================================================================================
miSCellaneous - a library of SuperCollider extensions (c) 2009-2020 Daniel Mayer 
======================================================================================

Version 0.24 contains these class and help files (SCDoc and old HTML system):


1.	Guide "Introduction to miSCellaneous": recommended starting point.

2.	VarGui:  a slider / player gui to set envir variables and synth controllers and 
	play synths, event patterns and tasks, see also "VarGui shortcut builds".
	"HS with VarGui", a specific tutorial about the combination of HS family and VarGui.

3.	General tutorials: "Event patterns and LFOs" contains an overview of
	related LFO-control setups with event patterns. "Event patterns and Functions" 
	is treating requirements of the control of EventStreamPlayers with VarGui.
	"Event patterns and array args" focusses on passing arrays to synths with patterns.
	"enum" is a general enumeration method suited for many combinatorial problems such as 
	listing subsets, partitions of integers, searching for paths within graphs etc.

4.	"PLx suite", dynamic scope variants of common Pattern classes for convenient 
	replacement. Can be used for shorter writing of Pbinds to be played with VarGui 
	and / or live coding, see "PLx and live coding with Strings".
	PLbindef and PLbindefPar as subclasses of Pdef allow replacement of 
	key streams in shortcut pseudomethod syntax.

5.	PSx stream patterns, Pattern variants that have a state and can remember their 
	last values. Can be used for recording streams and event streams (PS),
	data sharing between event streams (PSdup), comfortable defining of subpatterns
	with counted embedding, defining of recursive (event) sequences (PSrecur) and
	building loops on given Patterns/Streams with a variety of options, also for 
	live interaction (PSrecur).

6.	Event pattern classes for use with effects: PbindFx can handle arbitrary effect
	graphs per event, PmonoPar and PpolyPar follow the Pmono paradigm. 
	The tutorial "kitchen studies" contains the documented source code of a 
	fixed media piece using PbindFx for granulation.
	
7.	Further Pattern classes: PlaceAll, Pshufn, PsymNilSafe.
	PSPdiv is a dynamic multi-layer pulse divider based on Pspawner.

8.	"Buffer Granulation", a tutorial covering different approaches of implementing
	this synthesis method in SC (server versus language control, mixed forms),
	examples with VarGui, language-driven control with PLx suite patterns.
	The tutorial "Live Granulation" summarizes direct options without 
	explicit use of a buffer.
	
9.	A family of classes for the use of synth values in Pbind-like objects.
	Take Working with HS and HSpar as a starting point, see also
	HS, PHS, PHSuse, HSpar, PHSpar, PHSparUse, PHSplayer, PHSparPlayer, PHSusePlayer.

10.	EventShortcuts, a class for user-defined keywords for events and event patterns.
	The tutorial "Other event and pattern shortcuts" collects some further abbreviations,
	e.g. functional reference within events and event patterns, similar to Pkey.

11.	An implementation of Xenakis' Sieves as class and pattern family, see
	"Sieves and Psieve patterns" for an overview and examples. 
	
12.	FFT pseudo ugens for defining ranges by bin index: PV_BinRange and PV_BinGap.
	
13.	Smooth Clipping and Folding, a suite of pseudo ugens.	
	
14.	DX, a suite of pseudo ugens for crossfaded mixing and fanning according to
	demand-rate control. 
	
15.	Idev suite, patterns and drate ugen searching for numbers with integer distance from a 
	source pattern / signal.
	
16.	Nonlinear dynamics: Fb1 for single sample feedback / feedforward and GFIS for 
	generalized functional iteration synthesis.
	Fb1_ODE for ordinary differential equation integration. 
	
17.	ZeroXBufWr, ZeroXBufRd, TZeroXBufRd: pseudo ugens for analysis of zero crossings and 
	playing sequences of segments between them with demand rate control. Dwalk, a 	
	pseudo demand rate ugen that supports specific synthesis options with ZeroXBufRd.
	
			 
Many of the examples here are using patterns, resp. event patterns but do not cover 
their basic concepts. For a detailled description of SC's sequencing capabilities see 
James Harkins' Practical Guide to Patterns (PG_01_Introduction), the tutorial 
Streams-Patterns-Events (1-7) and the Pattern help files 
(Pattern, Pbind and the type-specific ones).

VarGui handles namespace separation by using different Environments.
So a gui for control of parametrized families of different types of objects can be 
built on the fly (e.g. a number of EventStreamPlayers from a single Pbind definition 
with snippets of functional code, a number of Function plots from a single 
parametric function definition etc.).
See Environment and Event helpfiles for the underlying concepts and 
"Event patterns and Functions" and "PLx suite" for their application to 
event patterns resp. EventStreamPlayers.


Requirements

At least SuperCollider version 3.6 but newer versions are recommended, 
with 3.6 you'd have to use Qt GUI kit, which is the only option anyway from 3.7 onwards. 
Unable to test on 3.5 anymore, code might work, anyway old help is still supported.

If you still use Cocoa or SwingOSC with these old SC versions,
you can take miSCellaneous 0.15b and add classes and help files from a 
newer version of miSCellaneous.

For using VarGui with EZSmoothSlider and EZRoundSlider you would need to 
install Wouter Snoei's wslib Quark, one buffer granulation
example using Wavesets depends on Alberto de Campo's Wavesets Quark. 

I tested examples on SC versions 3.6 - 3.11, 
on OS 10.8 - 10.13, Ubuntu 12.04 and Windows 7, 10; 
though not every platform / OS version / SC version combination.


SCDoc issues (SC 3.10)

With SC 3.10.0 - SC 3.10.2 there are issues with evaluating code in the 
help file examples (double evaluation).
For these versions you'd rather copy the help file code 
into scd files and run it there. 
Double evaluation has been fixed with 3.10.3 (August 2019).
With SC updates to 3.10 it might happen that help file examples are invisible.
In that case delete the Help folder which resides in one of these places, depending
where you have installed:

Platform.userAppSupportDir;
Platform.systemAppSupportDir;

Then restart SC.


Installation

By version 0.17 you can install either via the quarks extension management system or a 
downloaded zip from GitHub or my website.
Both methods shouldn't be combined, e.g. if you have already done a manual install 
before by placing a miSCellaneous folder in the Extensions folder, then remove 
it from there before the quarks install.

1.) Installation via Quarks

This requires a SC version >= 3.7, see the recommended ways to install here:

http://doc.sccode.org/Guides/UsingQuarks.html
https://github.com/supercollider-quarks/quarks

After a install of a newer version of miSCellaneous do a SCDoc update by

SCDoc.indexAllDocuments(true)


2.) Manual installation from a downloaded zip

You can download the newest version from 

https://github.com/dkmayer/miSCellaneous_lib

and the newest and all previous versions from here:

http://daniel-mayer.at

Copy the miSCellaneous folder into the Extensions folder and recompile 
the class library or (re-)start SC. If the Extensions folder doesn't exist 
you'd probably have to create it yourself. Check SC help for platform-specific 
conventions (or changes) of extension places.

Typical user-specific extension directories:

OSX:	~/Library/Application Support/SuperCollider/Extensions/
Linux:	~/.local/share/SuperCollider/Extensions/

Typical system-wide extension directories:

OSX:	/Library/Application Support/SuperCollider/Extensions/
Linux:	/usr/share/SuperCollider/Extensions/

You can check Extension directories with

Platform.userExtensionDir;
Platform.systemExtensionDir;

On Windows see the README file of SC for the recommended extensions path.
On Windows and OSX you might have to make the concerned folders visible,
if they aren't already. The miSCellaneous folder should be placed directly
into the Extensions folder, not into a subfolder.

After a install of a newer version of miSCellaneous do a SCDoc update by

SCDoc.indexAllDocuments(true)


License

miSCellaneous is distributed under the GNU Public License in accordance 
with SuperCollider. You should have received a copy of the 
GNU General Public License along with this program; 
if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.


Contact

Email:	daniel-mayer@email.de
URL:	http://daniel-mayer.at


Credits

Many thanks to James McCartney for developing SuperCollider, 
Alberto de Campo for showing me its capabilities,
Wouter Snoei for his nice slider classes in wslib, 
Nathaniel Virgo for his suggestions for feedback, 
David Pirrò for his hints concerning ODE integration,
James Harkins for his remarks on many things and 
the whole community for contributions and hints !


=====================================================================================


History


v0.24		2020-07-08

	.) Dwalk, demand rate ugen that supports specific synthesis options with ZeroXBufRd
	.) New examples #9, #10 in ZeroXBufRd help with applications of Dwalk
	.) New examples #8, #9 in TZeroXBufRd help (pulsar synthesis)	
	.) Bugfix related to PV_BinRange and PV_BinGap, usage in FFT chains enabled	
	

v0.23		2020-04-19

	.) ZeroXBufWr, ZeroXBufRd, TZeroXBufRd: playing half wavesets with demand rate control
	.) Remove doubled method lincurve_3_9, which caused a warning without harm
	.) AddEventTypes_PbindFx: use store instead of writeDefFile in private methods
	.) Minor fixes in help files	
	

v0.22		2019-08-14

	.) Fb1_ODE and related: ordinary differential equation integration 
	.) Fb1: now also runs at control rate, Fb1.new is equivalent to Fb1.ar	
	.) Fb1: minor change of forced graph ordering
	.) PbindFx: accept instruments/fxs given as Strings in pbindData key/value pairs
	.) Minor fixes in help files	
	

v0.21		2018-07-25

	.) Fb1: single sample feedback / feedforward 
	.) GFIS: generalized functional iteration synthesis	
	.) Redefined variables in Sieve, PSrecur and PSPdiv to enable inlining
	.) Minor fixes in help files	
	

v0.20		2018-05-21

	.) Idev suite, search for numbers with integer distance from a source
	.) Bug fix in Integer method lcmByFactors
	.) Minor fixes in help files	
	

v0.19		2017-11-22

	.) DX, a suite of pseudo ugens for crossfaded mixing and fanning
	.) Minor adaption of PbindFx event type	
	.) Minor fixes in help files	


v0.18		2017-09-26

	.) PLbindef / PLbindefPar: new features and implementation
	.) Minor fixes in PV_BinGap and PV_BinRange	
	.) Minor fixes in help files	


v0.17		2017-08-21

	.) Smooth Clipping and Folding, a suite of pseudo ugens
	.) Added MIDI learning feature for VarGui slider control
	.) Platform.miSCellaneousDirs, search for miSCellaneous folder
	.) Adapted PV_BinRange and PV_BinGap to do multichannel expansion
	.) Exchanged graphic examples in VarGui help (Qt now)	
	.) Minor fixes in help files	
	.) First version released via GitHub and as Quark


v0.16		2017-03-03

	.) Added PSPdiv, a dynamic multi-layer pulse divider based on Pspawner
	.) Added tutorial: Live Granulation
	.) Dropped miSCellaneous' b-branch: Cocoa and SwingOSC no longer supported
	.) Replaced OSCpathResponder by OSCFunc in classes VarGuiPlayerSection, 
	   HelpSynth and HelpSynthPar
	.) Minor fixes in help files	


v0.15		2017-01-04

	.) Guide "Introduction to miSCellaneous"
	.) kitchen studies: source code of the corresponding fixed media piece
	.) PV_BinRange, PV_BinGap: FFT pseudo ugens for defining bin ranges
	.) PbindFx help: new example 4a for defining implicit fx parallelism 
	.) Minor fixes (help files, typos)	
	.) Complemeting history information (tutorials)	


v0.14		2016-08-25

	.) Sieves and Psieve patterns, an implementation of Xenakis' sieves
	.) PLbindef / PLbindefPar: replacement of key streams in pseudomethod syntax 
	.) Tutorial: PLx and live coding with Strings
	.) PsymNilSafe, method symplay: avoid hangs with nil input
	.) EventShortcuts: reworking of replacement, new method eventShortcuts
	.) PbindFx: reworking of bus match check
	.) Minor fixes (help files, typos)	


v0.13		2015-10-28

	.) PbindFx: Event pattern class for effect handling on per-event base
	.) Minor fixes (help files, typos)	


v0.12		2015-05-03

	.) PmonoPar, PpolyPar:
	   Event pattern classes for parallel setting streams and effect handling
	.) PSloop: Pattern to derive loops from a given Pattern
	.) Reworked replacing options of PL list patterns: cutItems arg.
	.) PLn: PLx Pattern for replacing with protecting periods
	.) Added PLxrand, PLx version of Pxrand
	.) Changed implementations of PStream (PS) and PSrecur,
	   now the MemoRoutine isn't instantiated before embedding,
	   this enables use of PSx patterns with VarGui
	.) Fixed a bug in PL list patterns (PL_PproxyNth) that caused too early
	   replacements in certain cases
	.) Added method bufSeq for PStream
	.) Added missing help file of PLtuple
	.) Minor fixes (help files, typos)	


v0.11		2015-02-02

	.) Tutorial: Event patterns and array args
	

v0.10		2014-10-06

	.) Split into branches 0.10a (3.7 onwards) and 0.10b (from 3.4 up to 3.6.x),
	   with SC 3.7 SwingOSC and Cocoa aren't supported anymore.
	.) Class EventShortcuts and tutorial "Other event and pattern shortcuts"


v0.9		2014-02-18

	.) PSx stream patterns (classes and tutorial), based on class MemoRoutine
	.) Buffer Granulation Tutorial: new examples (1c - 1e, 3d)
	.) VarGui save dialog fix (was broken with 3.6)	
	.) Minor fixes (help files, typos)	


v0.8		2013-05-05

	.) Enumeration tutorial: method enum	


v0.7.1		2013-04-28

	.) Fix in PLseries and PLgeom	


v0.7		2012-08-31

	.) Buffer Granulation tutorial file	
	.) VarGui
		.) Support of wslib slider classes EZSmoothSlider and EZRoundSlider
		.) Color grouping options for synth and envir variable control
		.) Adapting EZSlider to ControlSpec step size 
		  (fixes certain rounding and jitter issues)
		.) Multiple slider handling with modifier keys: fixes and cleanup


v0.6		2012-05-19

	.) PLx dynamic scope pattern suite	
		.) Implementation of a number of common Patterns as PLx classes
		.) Tutorial PLx suite
		.) Changing examples in some previous help files accordingly
	.) Pstream, PlaceAll, Pshufn


v0.5		2012-03-18

	.) VarGui shortcut build methods	
		.) Use of SynthDef metadata and global ControlSpecs
		.) Tutorial	VarGui shortcut builds	
		.) Automatic Pbind generation
	.) Minor fixes in VarGui init procedure


v0.4		2011-12-27

	.) VarGui support for HS family classes
	.) Tutorial HS with VarGui	
	.) VarGui takes addAction as slider hook	
	.) Linux check (tested on Ubuntu):
		.) Fixed system time issue in HS family
		.) Specified VarGui appearance default parameters for platform and gui kit
	.) Supports SCDoc, the new SC help system
	.) Tutorial Event Patterns and Functions
	.) Tutorial Event patterns and LFOs
	.) Play methods of PHSx and PHSxPlayer now also take numbers as quant arg
	.) Minor fixes in HS family


v0.4beta	2011-08-18

	.) VarGui relaunch: 
		.) Player section for Synths, EventStreamPlayers and Tasks 
		.) Different player modes
		.) Button colors and background colors reflecting playing states 
		.) Handling groups of players and sliders with modifier keys
		.) Player action by mouse down or up (currently cocoa only)
		.) Variables can be set in different environments
		.) Latency setting, global and for synth player message bundling
		.) GUI appearance customization in size, arrangement and color
		.) Many other changes, e.g. arg conventions 
	.) Private extension methods get prefix miSC_


v0.3		2010-10-21

	.) VarGui:
		.) Arrayed synth control supported
		.) Slider update methods added
		.) Save dialog now uses unified gui class Dialog	
		.) Again compiling with SC 3.3 and 3.3.1


v0.2		2010-09-18

	.) Minor adaptions to SC 3.4
	.) Fixed time shifting issue


v0.1		2009-11-24

	.) VarGui, interface for envir variable and synth arg control
	.) HS / HSpar etc.: classes for the use of synth values in Pbind-like objects
	.) Tutorial Working with HS and HSpar
	

=====================================================================================


	