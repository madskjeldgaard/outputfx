OFX_Chain {

	classvar <allSources;
	classvar <sourceDicts;
	classvar <all;

	var <slotNames, <slotsInUse, <proxy, <sources;

	*initClass {
		allSources = ();
		sourceDicts = ();
		all = ();

		// this.addSpec;
	}

	// old style - not recommended: add list of sources
	// *add { |...args|
	// 	args.pairsDo { |srcName, source|
	// 		this.addSource(srcName, source);
	// 	}
	// }

	// new style - recommended: add source, level, specs together
	*add3 { |srcName, source, level, specs|
		var dict = this.atSrcDict(srcName);
		this.addSource(srcName, source);
		this.addLevel(srcName, level);
		this.addSpecs(srcName, specs, source);
		this.checkSourceDictAt(srcName);
	}

	*addSource { |srcName, source|
		var dict = this.atSrcDict(srcName);
		var srcFunc, paramNames;

		if (source.notNil) {
			// backwards compat - remove!
			allSources.put(srcName, source);

			dict.put(\source, source);
			srcFunc = this.prgetSourceFuncFromSource(source);
            
            // @FIXME this does not work with NamedControl
			paramNames = srcFunc.argNames.as(Array);
			paramNames.remove(\in);
			dict.put(\paramNames, paramNames);
		}
	}

	*addLevel { |srcName, level|
		var dict = this.atSrcDict(srcName);
		if (level.notNil) { dict.put(\level, level) }
	}

    *prgetSourceFuncFromSource{|source|
      if (source.isKindOf(Association)) { 
        ^source.value 
        } { 
          ^source 
        };
    }

    *prGetNamedControlSpecs{|source|
      var sourceFunc = this.prgetSourceFuncFromSource(source);
      ^sourceFunc.asSynthDef.asSynthDesc.specs.postln
    }

	*addSpecs { |srcName, specs, source|
		var dict = this.atSrcDict(srcName);
		var specDict, namedControlSpecs;

		if (specs.notNil) {
            
          specDict = dict[\specs] ?? { () };

          // @FIXME does not work because `in` is not understood as a proxy
          // namedControlSpecs = this.prGetNamedControlSpecs(source);
          // specDict = specDict ++ namedControlSpecs;

			dict.put(\specs, specDict);

			specs.keysValuesDo { |parkey, spec|
				var newspec;
				if (spec.isKindOf(Array)) { newspec = spec.asSpec };
				newspec = newspec ?? { 
                  spec.asSpec
                // this.getSpec(spec) ?? { spec.asSpec } 
                };
				if (newspec.isNil) {
					"%: spec conversion at % - % failed!\n".postf(this, srcName.cs,  parkey.cs)
				} {
					specDict.put(parkey, spec.asSpec);
				}
			}
		}
	}

	*checkDicts {
		sourceDicts.keysDo { |key| this.checkSourceDictAt(key) }
	}

	*checkSourceDictAt { |srcname|
		var dict = sourceDicts[srcname];
		var src = dict[\source];
		var paramNames = dict[\paramNames];

		paramNames.do { |name|
			var spec;
			if (dict[\specs].notNil) { spec = dict[\specs][name] };
			// spec = spec ?? { Chain.getSpec(name) };
			spec = spec ?? { name.asSpec };
			if (spec.isNil) {
				"*** Chain: % needs a spec for %!\n".postf(srcname, name);
			}
		}
	}

	*atSrcDict { |key|
		var sourceDict = sourceDicts[key];
		if (sourceDict.isNil) {
			// sourceDict = ().parent = this.getSpec;
            sourceDict = ();
			sourceDicts.put(key, sourceDict);
		};
		^sourceDict
	}

	*from { arg proxy, slotNames = #[];
		^super.new.init(proxy, slotNames)
	}

	*new { arg key, slotNames, numChannels, server;
		var proxy;
		var res = all.at(key);

		if(res.notNil) {
			if ([slotNames, numChannels, server].any(_.notNil)) {
				"*** %: cannot reset slotNames, numChannels, or server on an existing Chain."
				" Returning % as is.\n".postf(this, res)
			};
			^res
		};

		proxy = NodeProxy.audio(server ? Server.default, numChannels);
		res = this.from(proxy, slotNames);
		if (key.notNil) { all.put(key, res) };

		if(slotNames.notNil) { res.slotNames_(slotNames) };

		^res
	}

	key { ^all.findKeyForValue(this) }
	storeArgs { ^[this.key] }
	printOn { |stream| ^this.storeOn(stream) }

	init { |argProxy, argSlotNames|

		slotNames = Order.new;
		slotsInUse = Order.new;
		sources = ();
		sources.parent_(allSources);

		proxy = argProxy;
		if (proxy.key.notNil) { all.put(proxy.key, this) };

		this.slotNames_(argSlotNames);

		// proxy.addSpec;
		// proxy.getSpec.parent = this.class.getSpec;
	}

	// TODO: handle case where slots are currently playing!
	// for every slotsInUse, compare old vs new slots:
	// if active slot stays at its index, leave it running
	// if not, remove it at old index
	// and if it is in the new slotNames, add it again at its new index
	slotNames_ { |argSlotNames|
		slotNames.clear;
		argSlotNames.do { |name, i| slotNames.put(i + 1 * 10, name) };
	}

	add { |key, wet, func| 	// assume the index exists
		var index = slotNames.indexOf(key);
			// only overwrite existing keys so far.
		if (func.notNil, { this.sources.put(key, func) });
		this.addSlot(key, index, wet);
	}

	remove { |key|
	 	var oldSlotIndex = slotsInUse.indexOf(key);
		if (oldSlotIndex.notNil) { proxy[oldSlotIndex] = nil; };
		slotsInUse.remove(key);
	}

    specialKeyForFunc{|func, index|
      var prefix = (filter: "wet", mix: "mix", filterIn: "wet")[func.key];
      ^(prefix ++ index).asSymbol
    }

    setWet{|slotName, wet|
      if(this.isSlotActive(slotName), { 
        var index = this.slotIndexFor(slotName);

        var func = sources[slotName];

        this.setWetForFunc(func, index, wet)
      }, { 
        // "Slot % is not active".format(slotName).warn
      })
    }

    getWet{|slotName|
      if(this.isSlotActive(slotName), { 
        var index = this.slotIndexFor(slotName);
        var func = sources[slotName];

        ^this.getWetForFunc(func, index)
      }, { 
        ^nil
        // "Slot % is not active".format(slotName).warn
      })
    }

    randomizeSlot{|slotName|
      this.keysValuesAt(slotName).do{|pair| 
        var key = pair[0]; 
        var val = pair[1]; 
        var spec = this.getSpecForSourceAndParam(slotName, key);

        this.set(key, spec.map(rrand(0.0,1.0)))
      };
    }

    setWetForFunc{|func, index, wet|
      var specialKey = this.specialKeyForFunc(func, index);
      var prevVal = proxy.nodeMap.get(specialKey).value;
      if (wet.isNil) { wet = prevVal ? 0 };
      // proxy.addSpec(specialKey, \amp.asSpec);
      proxy.set(specialKey, wet);
    }

    getWetForFunc{|func, index|
      var specialKey = this.specialKeyForFunc(func, index);
      ^proxy.nodeMap.at(specialKey);
    }

    getSpecForSourceAndParam { |sourceName, paramName|
      ^OFX_Chain.atSrcDict(sourceName).specs[paramName] 
      ?? Spec.specs[paramName] 
      ?? [0.0,1.0].asSpec
    }

	addSlot { |key, index, wet|

		var func = sources[key];
		var srcDict = sourceDicts[key];

		if (func.isNil) { "Chain: no func called \%.\n".postf(key, index); ^this };
		if (index.isNil) { "Chain: index was nil.".postln; ^this };

		this.remove(key);
		slotsInUse.put(index, key);

        if (func.isKindOf(Association)) {
          this.setWetForFunc(func, index, wet)
        };

		// if (srcDict.notNil and: { srcDict.specs.notNil }) {
		// 	srcDict.specs.keysValuesDo { |param, spec| proxy.addSpec(param, spec) };
		// };
		proxy[index] = func;
	}

	setSlots { |keys, levels=#[], update=false|
		var keysToRemove, keysToAdd;
		if (update) {
			keysToRemove = slotsInUse.copy;
			keysToAdd = keys;
		} {
			keysToRemove = slotsInUse.difference(keys);
			keysToAdd = keys.difference(slotsInUse);
		};

		keysToRemove.do(this.remove(_));
		keysToAdd.do { |key, i| this.add(key, levels[i]) };
	}

		// forward basic messages to the proxy
	play { arg out, numChannels, group, multi=false, vol, fadeTime=1, addAction;
		proxy.play(out, numChannels, group, multi=false, vol, fadeTime, addAction)
	}

	playN { arg outs, amps, ins, vol, fadeTime, group, addAction;
		proxy.playN(outs, amps, ins, vol, fadeTime, group, addAction);
	}

	stop { arg fadeTime=1, reset=false;
		proxy.stop(fadeTime, reset);
	}

	end { arg fadeTime=1, reset=false;
		proxy.end(fadeTime, reset);
	}

	set { |... args| proxy.set(*args) }

    fadeTime_ { |time| proxy.fadeTime_(time) }

    xset { |... args| proxy.xset(*args) }

	clear {
		proxy.clear;
		all.removeAt(this.key);
	}

    // JIT gui support
    gui {
      ^OFX_ChainGui.new(this)
    }

	// gui { |numItems = 16, buttonList, parent, bounds, isMaster = false|
	// 	^ChainGui(this, numItems, parent, bounds, true, buttonList, isMaster);
	// }


	// introspection & preset support:
	activeSlotNames { ^slotsInUse.array }

    isSlotActive{|slotName| ^slotsInUse.array.indexOfEqual(slotName).isNil.not }

	slotIndexFor { |slotName| 
      ^slotNames.indexOf(slotName)
    }

	orderIndexFor { |slotName|
		var rawIndex = this.activeSlotNames.indexOf(slotName);

		if (rawIndex.isNil) {
			// "%: no active slot named %!\n".postf(this, slotName.cs);
            
			^nil
		};
		^slotsInUse.indices[rawIndex];
	}

	keysAt { |slotName|
		var orderIndex = this.orderIndexFor(slotName);
		var obj, names;

		if (orderIndex.isNil) { ^nil };

		obj = proxy.objects[orderIndex];
		names = obj.controlNames.collect(_.name);
		names.removeAll(proxy.internalKeys);
		^names
	}

    // @TODO
    getActiveParamValAt{|slotName, paramName|
      ^this.isSlotActive(slotName).if({
        var value = this.keysValuesAt(slotName)
        .select{|i| 
          i[0] == paramName 
        };

        if(value.isNil, { 
          "Chain: % is nil".format(paramName).warn 
        }, {
          value.flatten[1]
        });

      });

    }

    // @TODO does not work if slotNames is changed after the fact
    // Get the wetness control for a particular slot
    getWetControlKey{|slotName|
      ^this.keysValuesAt(slotName).select({|pair| var paramName = pair[0]; paramName.asString[0..2] == "wet" }).flatten[0]
    }

	keysValuesAt { |slotName|
		var keys = this.keysAt(slotName);
		if (keys.isNil) { ^nil };
		^proxy.getKeysValues(keys);
	}

	getCurr { |except|
		var slotsToGet = this.activeSlotNames.copy;
		slotsToGet.removeAll(except);
		^slotsToGet.collect { |slotName|
			slotName -> this.keysValuesAt(slotName);
		}
	}
}
