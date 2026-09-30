import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../widgets/models/widget_content.dart';
import '../../widgets/providers/widget_providers.dart';
import 'controls.dart';

/// Extra events for the "Up Next" countdown list.
class EventsControls extends ConsumerStatefulWidget {
  const EventsControls({super.key});

  @override
  ConsumerState<EventsControls> createState() => _EventsControlsState();
}

class _EventsControlsState extends ConsumerState<EventsControls> {
  final _title = TextEditingController();
  DateTime? _date;

  @override
  void dispose() {
    _title.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _date ?? now,
      firstDate: DateTime(now.year - 5),
      lastDate: DateTime(now.year + 50),
    );
    if (picked != null) setState(() => _date = picked);
  }

  void _add() {
    final title = _title.text.trim();
    final date = _date;
    if (title.isEmpty || date == null) return;
    ref
        .read(contentProvider.notifier)
        .update(
          (c) => c.copyWith(
            events: [
              ...c.events,
              CountdownEvent(title: title, date: date),
            ],
          ),
        );
    _title.clear();
    setState(() => _date = null);
  }

  void _remove(int index) => ref
      .read(contentProvider.notifier)
      .update((c) => c.copyWith(events: [...c.events]..removeAt(index)));

  @override
  Widget build(BuildContext context) {
    final events = ref.watch(contentProvider).events;
    final format = DateFormat('d MMM y');
    return Section(
      title: 'More events (Up Next list)',
      child: Column(
        children: [
          for (final (i, e) in events.indexed)
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(e.title),
              subtitle: Text(format.format(e.date)),
              trailing: IconButton(
                tooltip: 'Remove',
                icon: const Icon(Icons.close, size: 20),
                onPressed: () => _remove(i),
              ),
            ),
          TextField(
            controller: _title,
            maxLength: 32,
            decoration: const InputDecoration(labelText: 'Event'),
            onSubmitted: (_) => _add(),
          ),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: _pickDate,
                  child: Text(
                    _date == null ? 'Pick date' : format.format(_date!),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              FilledButton(onPressed: _add, child: const Text('Add')),
            ],
          ),
        ],
      ),
    );
  }
}

/// Items for the checklist note; the checked state is toggled here.
class ChecklistControls extends ConsumerStatefulWidget {
  const ChecklistControls({super.key});

  @override
  ConsumerState<ChecklistControls> createState() => _ChecklistControlsState();
}

class _ChecklistControlsState extends ConsumerState<ChecklistControls> {
  final _text = TextEditingController();

  @override
  void dispose() {
    _text.dispose();
    super.dispose();
  }

  void _add() {
    final text = _text.text.trim();
    if (text.isEmpty) return;
    ref
        .read(contentProvider.notifier)
        .update(
          (c) => c.copyWith(
            checklist: [
              ...c.checklist,
              ChecklistItem(text: text),
            ],
          ),
        );
    _text.clear();
  }

  void _toggle(int index) => ref
      .read(contentProvider.notifier)
      .update(
        (c) => c.copyWith(
          checklist: [
            for (final (i, item) in c.checklist.indexed)
              i == index ? item.toggled() : item,
          ],
        ),
      );

  void _remove(int index) => ref
      .read(contentProvider.notifier)
      .update((c) => c.copyWith(checklist: [...c.checklist]..removeAt(index)));

  @override
  Widget build(BuildContext context) {
    final items = ref.watch(contentProvider).checklist;
    return Section(
      title: 'Checklist items',
      child: Column(
        children: [
          for (final (i, item) in items.indexed)
            CheckboxListTile(
              contentPadding: EdgeInsets.zero,
              controlAffinity: ListTileControlAffinity.leading,
              value: item.done,
              onChanged: (_) => _toggle(i),
              title: Text(
                item.text,
                style: item.done
                    ? const TextStyle(decoration: TextDecoration.lineThrough)
                    : null,
              ),
              secondary: IconButton(
                tooltip: 'Remove',
                icon: const Icon(Icons.close, size: 20),
                onPressed: () => _remove(i),
              ),
            ),
          TextField(
            controller: _text,
            maxLength: 60,
            decoration: InputDecoration(
              labelText: 'New item',
              suffixIcon: IconButton(
                tooltip: 'Add',
                icon: const Icon(Icons.add),
                onPressed: _add,
              ),
            ),
            onSubmitted: (_) => _add(),
          ),
        ],
      ),
    );
  }
}
