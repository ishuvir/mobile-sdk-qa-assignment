# Open Questions

**Ishuvir Singh** - 8 October 2026


## Questions I would ask before building this out

1. **Which parts of the SDK have no tests today?** I want to know where the gaps are before I add
   anything.
2. **What broke in the last few releases, and how did we find out - a customer, or CI?** That tells me
   what the gates are actually catching.
3. **Can a test log in without a real user and a real device?** I would like to seed a session in a
   debug build so login and refresh specs do not need real credentials.
4. **Which app versions are still supported, and who decides when one can be dropped?** That is what
   sets the compatibility matrix.
5. **When a test fails in the apps team's pipeline, how do we find out whose module caused it?** This
   is the brief's "source of a failure is hard to identify" problem, asked plainly.
6. **What has stopped a release before?** The gates should come from real incidents, not a template.

The answers to 3 and 5 change the shape of most of what I have proposed, so I would want those two
early.

---
