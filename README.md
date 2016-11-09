# onyx-blueprint

For the moment, this repository supports a few aspects: the development of Onyx tutorial content, a set of components used to make that content interactive, plus the engine that drives the rendering and layout of those components.

In the future, __much will change__. See issues #1 and #7.

#### Current anatomy

* `src` - The `onyx-blueprint` namespace
  * The rendering engine, based on om.next
  * Common UI components
* `example_src` - The self-documenting `example.showcase`
* `tutorial_src` - The in-progress `onyx-tutorial`
  * An example topic: `onyx-tutorial.workflow`
* `resources/public` - Html, CSS and generated JS artifacts

#### Getting started

A good entry point is the blueprint showcase.

```sh
lein figwheel showcase-dev

# or, use the figwheel-sidecar approach
```

#### Tutorial development

```
[TBD, based on team conversation.]
```



