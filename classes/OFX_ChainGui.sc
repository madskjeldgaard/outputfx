/*

The gui is controlled has a timer (SkipJack) attached. It checks whether the gui objects are out of sync with the proxy. If it is, it will copy the proxy's values to the gui objects' data space.

TODO: 
- Does not work if one of slots is inactive
- Make reactive to slotnames changing
- Check for new parameters / items (is that even possible after defining the proxychain?)

*/

OFX_ChainGui{
  var <window, chain, <guiObjects, <guiData, <skipjack, <isMain, <slotNames, <slotsInUse;

  var title, sourceKeys, slotSections,transportButtons, presetButtons;

  *new{|proxychain, isMainOutput=false| 
    ^super.new.init(proxychain, isMainOutput)
  }

  init{|proxychain, isMainOutput|
    chain = proxychain;
    isMain = isMainOutput;

    this.makeGui();

    window.front();
  }

  winName{
    ^chain.key.asString;
  }

  makeGui{

    var layout;

    window = window ?? {Window.new(name: this.winName)};
    window.view.removeAll;

    guiObjects = IdentityDictionary.new;
    guiData = guiData ?? { IdentityDictionary.new };

    slotNames = chain.slotNames.copy();
    sourceKeys = OFX_Chain.allSources.keys;

    title = StaticText.new()
    .string_(chain.key)
    .font_(Font.default.bold_(true));

    slotSections =  VLayout(
      *slotNames.asArray.collect{|sourceKey| 
        this.makeSlotSection(sourceKey)
      }
    );

    // @TODO
    // transportButtons = if(isMain, { nil }, {
    //   this.makeTransportSection() 
    // });

    // @TODO
    // presetButtons = this.makePresetSection();

    this.makeGuiData();
    skipjack =skipjack ?? {this.makeSkipjack()};

    this.checkAllActiveSlots();
    layout = VLayout(title, transportButtons, presetButtons, slotSections);
    window.layout_(layout);

  }

  makeSlotSection{|sourceKey|
    var toggleSlotButton = Button.new() 
    .states_([
      [sourceKey, Color.black, Color.red],
      [sourceKey, Color.black, Color.green],
    ])
    .value_(
      if(chain.isSlotActive(sourceKey), { 1 }, { 0 })
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
    var wetness = Slider.new()
    .orientation_(\horizontal)
    .action_({|obj| 
      chain.setWet(sourceKey, obj.value)
      // OFX_Chain.atSrcDict(sourceKey).postln.level = obj.value
    });

    var parameterSliders = VLayout(
      *this.slidersForSlot(sourceKey)
    );

    // @TODO: make blank space flexible 
    var blankSpace = 12;


    // Set slider after the fact
    wetness.valueAction_(currentLevel);

    // @FIXME Doesn't actually work
    // var blankSpace = [nil];
    ^VLayout(
      HLayout(toggleSlotButton, wetness), parameterSliders, blankSpace
    )
  }

  makePresetSection{
    ^HLayout(*[
      // Save
      Button.new()
      .states_([["save"], ["save"]])
      .action_({|obj| if(obj.value == 1, { 
        "not implemented yet".warn
      })}),

      // Load
      Button.new()
      .states_([["load"], ["load"]])
      .action_({|obj| if(obj.value == 1, { "not implemented yet".warn })}),

      // Open
      Button.new()
      .states_([["open"], ["open"]])
      .action_({|obj| if(obj.value == 1, { "not implemented yet".warn })}),

    ])
  }

  makeTransportSection{
    ^HLayout(*[

      // Play
      Button.new()
      .states_([["play"], ["play"]])
      .action_({|obj| if(obj.value == 1, { chain.play })}),

      // Stop
      Button.new()
      .states_([["stop"], ["stop"]])
      .action_({|obj| if(obj.value == 1, { chain.stop })}),

      // Clear
      Button.new()
      .states_([["clear"], ["clear"]])
      .action_({|obj| if(obj.value == 1, { chain.clear })}),

      // End
      Button.new()
      .states_([["end"], ["end"]])
      .action_({|obj| if(obj.value == 1, { chain.end })}),
    ])
  }

  makeGuiData {
    chain.slotNames.do{|slotName|
      var proxyValues = chain.keysValuesAt(slotName);

      // "Making gui data for slot %".format(slotName).postln;
      // if(proxyValues.isNil or: { proxyValues.isEmpty }, { "Nothing in proxyValues for %".format(slotName).warn });
      proxyValues.do{|pair|
        var key = pair[0];
        var val = pair[1];
        // "Adding %, % to guiData".format(key, val).postln;
        guiData[slotName][key] = val ?? 0
      }
    }
  }

  makeSkipjack {
    SkipJack.new(
      updateFunc: {
        if(this.isProxyInSync().not, { 
          // "Remaking gui".postln;
          this.makeGui;
        });

        if(this.checkSlotActivity(), { this.makeGui });

        this.checkAllActiveSlots()
      },  
      dt: 0.1,  
      stopTest: { window.isNil or: { window.isClosed } },  
      name: chain.key,  
    );
  }

  checkSlotActivity{
    var chainSlots = chain.slotsInUse;

    if(slotsInUse != chainSlots,{ 
      // "New slots became active. Old slots: %, new slots: %".format(slotsInUse, chainSlots).postln;
      slotsInUse = chain.slotsInUse.copy;
      ^true
    }, { ^false });
  }

  isProxyInSync{
    var status;

    // Did the slot chain setup change?
    if(
      slotNames != chain.slotNames, {
        ^false 
      }, { ^true });

      
    // Check if slotnames changed
    // Check if parameters changed
    // Check if specs changed

  }

  // Checks whether something is new in a slot (whether it was updated manually outside the gui) and if so updates the gui elements
  checkSlot {|sourceName|
    var proxyValues = chain.keysValuesAt(sourceName);

    proxyValues.do{|pair|
      var key = pair[0];
      var proxyval = pair[1];
      var val = guiData[sourceName][key];
      var guiObject = guiObjects[sourceName][key];
      // "guiObject: %".format(~guiObjects[sourceName][key]).postln;
      // "sourcename: %, key: %, val: %".format(sourceName, key, val).postln;

      if(guiObject.isNil, { 
        // "Chain: No gui object for key % in slot %".format(key, sourceName).warn
      }, { 
        // Only have to update the gui - the other way around is always in sync because an action is called
        if(proxyval != val /*or: { val != guiObject[\valueLabel].value }*/, {
          var spec = this.getSpecForSourceAndParam(sourceName, key);
          var unmapped = spec.unmap(proxyval);

          // "val: %, proxyval: %".format(val, proxyval).postln;
          guiData[sourceName][key] = proxyval;
          guiObject.slider.value_(unmapped);
          guiObject.valueLabel.value_(unmapped);
        })

      })
    }
  }

  checkAllActiveSlots {
    slotNames.do{|slotName| 
      this.checkSlot(chain, slotName) 
    }
  }

  getSpecForSourceAndParam { |sourceName, paramName|
    ^OFX_Chain.atSrcDict(sourceName).specs[paramName] 
    ?? Spec.specs[paramName] 
    ?? [0.0,1.0].asSpec
  }

  slidersForSlot {|sourceName| 
    var sliders, params, objectDict;
    params = OFX_Chain.atSrcDict(sourceName).paramNames;
    objectDict = IdentityDictionary.new;

    guiData[sourceName] = guiData[sourceName] ?? IdentityDictionary.new;

    // if(params.isEmpty, { "No parameters found for %".format(sourceName).warn });

    sliders = params.collect{|paramName|
      var spec = this.getSpecForSourceAndParam(sourceName, paramName) ;
      var currentLevel = spec.unmap(
        chain.getActiveParamValAt(sourceName, paramName) ? 0
      );

      var valueLabel = NumberBox.new()
      .value_(currentLevel);

      var slider = Slider.new()
      .orientation_(\horizontal)
      .value_(currentLevel)
      .action_({|obj| 
        var val = spec.map(obj.value);

        valueLabel.value_(val);

        guiData[sourceName][paramName] = val;

        chain.proxy.set(paramName, val);
      });

      var label = StaticText.new()
      .string_(paramName);

      // Used for synchronising with the value of the proxy later on
      objectDict[paramName] = (valueLabel: valueLabel, slider: slider);

      VLayout(
        // label, 
        HLayout(
          [label, stretch: 2],
          [slider, stretch: 5], 
          [valueLabel, stretch: 1]
        )
      )
    };

    guiObjects.put(sourceName, objectDict);

    ^sliders
  }
}

