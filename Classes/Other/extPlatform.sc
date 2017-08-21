

+ Platform {
	*miSCellaneousDirs { |withWarnings = true|
		var dirs = (Platform.userExtensionDir +/+ "miSCellaneous*").pathMatch ++
			(Platform.systemExtensionDir +/+ "miSCellaneous*").pathMatch ++
			(Platform.userExtensionDir +/+ "miSCellaneous*" +/+ "miSCellaneous*").pathMatch ++
			(Platform.systemExtensionDir +/+ "miSCellaneous*" +/+ "miSCellaneous*").pathMatch ++
			(Platform.userAppSupportDir +/+ "quarks" +/+ "miSCellaneous*").pathMatch ++
			(Platform.systemAppSupportDir +/+ "quarks" +/+ "miSCellaneous*").pathMatch ++
			(Platform.userAppSupportDir +/+ "downloaded-quarks" +/+ "miSCellaneous*").pathMatch ++
			(Platform.systemAppSupportDir +/+ "downloaded-quarks" +/+ "miSCellaneous*").pathMatch;
		dirs = dirs.select { |p| PathName(p).isFolder and: { PathName(p +/+ "Classes").isFolder } };
		withWarnings.if {
			case
			{ dirs.size == 0 }{
				"\nWARNING: no directory beginning with name 'miSCellaneous' found within extension directories\n".postln;
			}
			{ dirs.size > 1 }{
				("\nWARNING: more than one directory beginning with name 'miSCellaneous' found within extension directories,\n" ++
				"there might be a discrepancy\n").postln;
			}
			{ true }{ }
		};
		^dirs
	}
}

