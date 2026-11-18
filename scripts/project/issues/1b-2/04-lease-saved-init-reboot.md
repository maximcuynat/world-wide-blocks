Title: Save the lease and ask for the same address after a restart (INIT-REBOOT)
Labels: type: feature, area: engine, priority: medium
Milestone: lot 1b-2

A PC that had an address before a server restart asks directly for its former address (INIT-REBOOT state of RFC 2131), so its lease must be saved.

- [ ] Lease of each PC saved with the network (data/<mod_id>/network.dat), compatible with the worlds of lot 1b-1
- [ ] After a restart: REQUEST for the saved address; NAK from the box: back to DISCOVER
- [ ] Game test: save, reload, same address

Noted in `tasks/todo.md` before lot 1b-2 ("ne pas oublier l'état INIT-REBOOT").
