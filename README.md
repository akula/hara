# hara 

[![Join the chat at https://gitter.im/zcaudate/hara](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/zcaudate/hara?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/zcaudate/hara.png?branch=master)](https://travis-ci.org/zcaudate/hara)

General purpose utilities library. Please see [finding a middle ground](http://z.caudate.me/finding-a-middle-ground/) for motivations and reasoning.

## Whats New

#### 2.1.11
- bugfix for `hara.reflect`, added `hara.object` namespace

#### 2.1.10
- Fixed all reflection warnings

#### 2.1.8
- Reworked `hara.reflect` to use only functions, moved helper macros into vinyasa 

#### 2.1.5
- Fix for `hara.component` to work with none record-based components

#### 2.1.4

- Moved [iroh](http://github.com/zcaudate/iroh) to `im.chit/hara.reflect`
- Added initialisers for `hara.component`

## Installation

Add to project.clj dependencies:

```clojure
[im.chit/hara "2.1.11"]

or

[im.chit/hara.<PACKAGE> "2.1.11"]

or

[im.chit/hara.<PACKAGE>.<NAMESPACE> "2.1.11"]
```

Where `PACKAGE` and `NAMESPACE` can be seen from the [API Documentation](http://docs.caudate.me/hara/). Please see documentation for examples of usage.

## License

Copyright © 2015 Chris Zheng

Distributed under the MIT License
