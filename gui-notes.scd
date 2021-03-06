/*

TODO: 
- Check for new parameters / items (is that even possible after defining the proxychain?)

*/
(

w = Window.new();

~guiObjects = IdentityDictionary.new;
~guiData = IdentityDictionary.new;

~makeGuiData = {|chain|
	chain.activeSlotNames.do{|slotName|
		var proxyValues = chain.keysValuesAt(slotName);
		proxyValues.do{|pair|
			var key = pair[0];
			var val = pair[1];
			~guiData[slotName][key] = chain.getActiveParamValAt(slotName, key)
		}
	}
	
};

// Checks whether something is new in a slot (whether it was updated manually outside the gui) and if so updates the gui elements
~checkSlot = {|chain, sourceName|
	var proxyValues = chain.keysValuesAt(sourceName);

	// Convert to dictionary (@FIXME: Ineffective! )
	// proxyValues = proxyValues.flatten.asDict;

	proxyValues.do{|pair|
		var key = pair[0];
		var proxyval = pair[1];
		var val = ~guiData[sourceName][key];
		var guiObject = ~guiObjects[sourceName][key];
		// "guiObject: %".format(~guiObjects[sourceName][key]).postln;
		// "sourcename: %, key: %, val: %".format(sourceName, key, val).postln;

		if(guiObject.isNil, { 
			// "Chain: No gui object for key % in slot %".format(key, sourceName).warn
		}, { 
			// Only have to update the gui - the other way around is always in sync because an action is called
			if(proxyval != val /*or: { val != guiObject[\valueLabel].value }*/, {
				var spec = ~getSpecForSourceAndParam.value(sourceName, key);
				var unmapped = spec.unmap(proxyval);

				"val: %, proxyval: %".format(val, proxyval).postln;
				~guiData[sourceName][key] = proxyval;
				guiObject.slider.value_(unmapped);
				guiObject.valueLabel.value_(unmapped);
			})

		})
	};

};

// The above for all active slots
~checkAllActiveSlots = {|chain|
	chain.activeSlotNames.do{|slotName| ~checkSlot.value(chain, slotName) }
};

~makeSkipjack = {|chain|
	SkipJack.new(
		updateFunc: {
			~checkAllActiveSlots.value(chain)
		},  
		dt: 0.1,  
		stopTest: { w.isNil or: { w.isClosed } },  
		name: chain.key,  
	);
};

~getSpecForSourceAndParam = { |sourceName, paramName|
	OFX_Chain.atSrcDict(sourceName).specs[paramName] 
	?? Spec.specs[paramName] 
	?? [0.0,1.0].asSpec;
};

// Create sliders, labels and number box for a parameter in a slot
~slidersForSlot = {|chain, sourceName| 
	var sliders, params, objectDict;
	params = OFX_Chain.atSrcDict(sourceName).paramNames;
	objectDict = IdentityDictionary.new;

	~guiData[sourceName] = ~guiData[sourceName] ?? IdentityDictionary.new;

	sliders = params.collect{|paramName|
		var spec = ~getSpecForSourceAndParam.value(sourceName, paramName) ;
		var currentLevel = spec.unmap(
			chain.getActiveParamValAt(sourceName, paramName)
		);

		var valueLabel = NumberBox.new(w)
		.value_(currentLevel);

		var slider = Slider.new(w)
		.orientation_(\horizontal)
		.value_(currentLevel)
		.action_({|obj| 
			var val = spec.map(obj.value);
			
			valueLabel.value_(val);
			~guiData[sourceName][paramName] = val;
			chain.proxy.set(paramName, val);
		});

		var label = StaticText.new(w)
		.string_(paramName);

		// Used for synchronising with the value of the proxy later on
		objectDict[paramName] = (valueLabel: valueLabel, slider: slider);

		VLayout(
			label, 
			HLayout(
				[slider, stretch: 5], 
				[valueLabel, stretch: 1]
			)
		)
	};

	~guiObjects.put(sourceName, objectDict);

	sliders
};

// Make the GUI for the entire ProxyChain
~makeChainGui = {|chain, isMainOutput=false|
	var current = chain.getCurr();

	var title = StaticText.new(w)
	.string_(chain.key)
	.font_(Font.default.bold_(true));

	var sourceKeys = OFX_Chain.allSources.keys;
	var slotButtons = VLayout(
		*sourceKeys.asArray.collect{|sourceKey, params| 

			// var parameterPairs = if(
			// 	current[sourceKey].isNil, { 
			// 	nil 
			// }, {
			// 	current.at( sourceKey ).value.postln  
			// });

			var button = Button.new(w) 
			.states_([
				[sourceKey, Color.black, Color.red],
				[sourceKey, Color.black, Color.green],
			])
			.value_(
				chain.isSlotActive(sourceKey).asInteger
			)
			.action_({|obj| 
				if(obj.value == 1, {
					// Add source
					chain.add(sourceKey)
				}, {
					// Remove source
					chain.remove(sourceKey)
				})
			});

			var currentLevel = OFX_Chain.atSrcDict(sourceKey).level;
			var wetness = Slider.new(w)
			.orientation_(\horizontal)
			.value_(currentLevel)
			.action_({|obj| 
				chain.setWet(sourceKey, obj.value)
				// OFX_Chain.atSrcDict(sourceKey).postln.level = obj.value
			});

			var parameterSliders = VLayout(
				*~slidersForSlot.value(chain, sourceKey)
			);

			// @TODO: make blank space flexible 
			var blankSpace = 12;
			// @FIXME Doesn't actually work
			// var blankSpace = [nil];
			VLayout(
				HLayout(button, wetness), parameterSliders, blankSpace
			)
		}
	);

	var transportButtons = if(isMainOutput, { nil }, {
		HLayout(*[

			// Play
			Button.new(w)
			.states_([["play"], ["play"]])
			.action_({|obj| if(obj.value == 1, { chain.play })}),

			// Stop
			Button.new(w)
			.states_([["stop"], ["stop"]])
			.action_({|obj| if(obj.value == 1, { chain.stop })}),

			// Clear
			Button.new(w)
			.states_([["clear"], ["clear"]])
			.action_({|obj| if(obj.value == 1, { chain.clear })}),

			// End
			Button.new(w)
			.states_([["end"], ["end"]])
			.action_({|obj| if(obj.value == 1, { chain.end })}),
		])
	});

	var presetButtons = HLayout(*[
		// Save
		Button.new(w)
		.states_([["save"], ["save"]])
		.action_({|obj| if(obj.value == 1, { 
			"not implemented yet".warn
		})}),

		// Load
		Button.new(w)
		.states_([["load"], ["load"]])
		.action_({|obj| if(obj.value == 1, { "not implemented yet".warn })}),

	]);

	w.layout_(VLayout(title, transportButtons, presetButtons, slotButtons));
	~makeGuiData.value(chain);
	~skipjack = ~makeSkipjack.value(chain);

	~checkAllActiveSlots.value(chain);
	w.front();
};

~makeChainGui.value(m.proxyChain, true);

)

Window.closeAll;
