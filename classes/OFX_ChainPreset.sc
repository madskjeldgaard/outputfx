OFX_ChainPreset {
	classvar <all;
	var <key, <chain, <proxy, <settings, <currSet;
	var <>exceptedSlots;

	*initClass {
		all = ();
	}

	storeArgs { ^[key] }
	printOn { |stream| ^this.storeOn(stream) }

	*new { |keyOrObj|
		var res, key;
        var newChain;
		if (keyOrObj.isKindOf(OFX_Chain)) {
			key = keyOrObj.key;

		} { 
          if(keyOrObj.isKindOf(OFX_OutputFX), { 
            key = \outputfx; 
          }, { 
            key = keyOrObj 
          })
        };
		res = all[keyOrObj];
		if (res.notNil) { ^res };

        newChain = if(keyOrObj.isKindOf(OFX_OutputFX), { 
          keyOrObj.proxyChain;
        }, {
          OFX_Chain(key)
        });

		// what if no proxychain found for key?
		^this.newCopyArgs(key, newChain).init
	}

	init {
		all.put(key, this);
		proxy = chain.proxy;

		settings = OFX_StoredList(key, List[], ".ofxcpreset");
	}

	slotsToStore {
		var slotsToStore = chain.slotNames.array.copy;
		slotsToStore.removeAll(exceptedSlots);
		^slotsToStore
	}

	settingNames { ^settings.names }
	settingIndex { |setName| ^settings.indexOf(setName) }

	getCurr {
		var activeSlotsToStore = this.slotsToStore.sect(chain.activeSlotNames.copy);
		^activeSlotsToStore.collect { |slotName|
			slotName -> chain.keysValuesAt(slotName)
		}
	}

	addSet { |setName|
		settings.add(setName, this.getCurr.postln);
	}
	removeSet { |setName|
		settings.removeAt(setName);
	}

	writeSettings { settings.write }
	loadSettings {|path, keep = false, doneFunc| settings.read(path, keep, doneFunc) }

	// HH added methods for setCurrByIndex, stepCurr, and, later, xset!
	setCurrByIndex { |index=0, absolute = true, except|
		var name = settings.names @@ index;
		// "setCurrByIndex: new name: %\n".postf(name);
		this.setCurr(name, absolute = true, except)
	}

	stepCurr { |incr=1|
		var currIndex = this.settingIndex(currSet) ? 0;
		var newIndex = currIndex + incr % settings.list.size;
		// "stepCurr: currIndex: %, newIndex %\n".postf(currIndex, newIndex);
		this.setCurrByIndex(newIndex);
	}


	// set immediately for now
	setCurr { |setName, absolute = true, except|
		var newset = settings.at(setName) ?? { var index = this.settingIndex(setName.asString); settings.at(index)};
		var keysValues, newSlotNames, slotNamesToRemove;
		if (newset.isNil) {
			"%: no preset named %!\n".postf(this, setName.cs);
			^this
		};

		currSet = setName;

		newSlotNames = newset.value.collect(_.key);
		"%: preset % activates these slots: %\n".postf(this, setName, newSlotNames);

		// add in all newSlotNames with their proper wet settings ...
		newset.value.do { |assoc|
			var keysVals = assoc.value;
			chain.add(assoc.key, keysVals[0][1]);
			chain.proxy.set(*keysVals.drop(1).flat);
		};

		// stop all other slotNames except protected ones:
		if (absolute) {
			slotNamesToRemove = chain.slotNames.array.copy;
			slotNamesToRemove.removeAll(newSlotNames);
			slotNamesToRemove.removeAll(exceptedSlots);
			slotNamesToRemove.removeAll(except);
			// "%: preset % stops slots: %\n".postf(this, setName, slotNamesToRemove);
			slotNamesToRemove.do { |name| chain.remove(name) };
			chain.proxy.cleanNodeMap;
		};
	}

	storeDialog { |name, loc|
		// good default name ? ...
		// loc can come from gui location
		settings.storeDialog(nil, this.getCurr, loc);
	}

	deleteDialog { |loc| settings.deleteDialog(loc) }
}
