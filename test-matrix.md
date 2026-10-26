# RetroFlow Business Rule Matrix

## Sources and Conventions

- Sources: RetroFlow Project Brief v1.0 supplied in the conversation, and [decisions.md](decisions.md), including its opening length table and sections 1-6.
- References to `brief` identify its named sections. Unqualified section numbers and the opening length table refer to the decisions document.
- Where the sources disagree, the row is flagged instead of silently choosing one. The brief establishes status, feedback-type and priority values, the no-reopening rule, and trusted access without authentication.
- No rules, enum definitions, exception classes, endpoints, or behavior have been taken from code, tests, or the README.
- Operations below are logical operations, not additional endpoint commitments. In particular, retrospective editing has no specified route.
- Each row covers one rule in one operation. Repeated rules are deliberately repeated across operations.
- `Not specified` in the exception column means the documents do not name a Java exception class. It does not mean the operation should return null or silently succeed.
- `None` means no exception is expected for that situation. Success assumes all other applicable rules are satisfied.
- HTTP codes follow section 5: create 201, read/update 200, delete 204, validation/business-rule failure 400, missing resource 404, unsupported method 405.
- Rows marked **DECIDED** were previously open questions. Each records the decision taken; these decisions are not in the brief or decisions.md and should be confirmed with the client.
- A missing/null request value, invalid syntax, and business-rule failures are distinguished where their treatment needs separate decisions. Error precedence when multiple rules fail is not specified.
- No authentication does not remove the documented team-membership checks. Requester/member IDs are supplied identifiers, not proof of an authenticated identity.
- Per the brief's definition of done, business-rule expectations in every row must be enforced in the service layer, not only in controllers. Java/Spring/JPA/build tooling, frontend exclusion, startup and test-coverage requirements are delivery constraints rather than additional domain rules.

## Teams and Members

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
| --- | --- | --- | --- | --- | --- |
| Team name is required and nonblank (section 1) | Create team | Name is missing, null, empty, or whitespace-only | Reject request | Not specified | 400 |
| Team name contains only alphanumeric characters and whitespace (section 1) | Create team | Name contains punctuation or other disallowed characters | Reject request | Not specified | 400 |
| Team name length is 1-255 (opening table; section 1) | Create team | Name exceeds 255 characters | Reject request | Not specified | 400 |
| A team must always have at least one member (section 1) | Create team | No members are supplied | Reject request | Not specified | 400 |
| Member names are required and nonblank (section 1) | Create team | Any member name is missing, null, empty, or whitespace-only | Reject request | Not specified | 400 |
| Member names contain only alphanumeric characters and whitespace (section 1) | Create team | Any member name contains disallowed characters | Reject request | Not specified | 400 |
| Member name length is 1-255 (opening table; section 1) | Create team | Any member name exceeds 255 characters | Reject request | Not specified | 400 |
| Team names are unique (section 1) | Create team | Another team has the same name | Reject request | Not specified | 400 |
| Name comparisons ignore case (section 1; section 6) | Create team | Team name differs from an existing name only by case | Treat names as equal; reject duplicate | Not specified | 400 |
| Name comparisons ignore surrounding whitespace (section 1; section 6) | Create team | Team name differs from an existing name only by surrounding whitespace | Treat names as equal; reject duplicate | Not specified | 400 |
| Names are stored exactly as submitted (section 6) | Create team | Valid team and member names contain surrounding whitespace or mixed case | Preserve submitted values; normalize only for comparisons | None | 201 |
| Teams retain member names (brief: Teams; section 1) | Create team | Members are supplied as names | Store their names; section 1 additionally establishes member UUID identity | None | 201 |
| Members are identified by unique ID, not name (section 1) | Create team | Two supplied members have the same name | Allow separate members with separate IDs | None | 201 |
| Duplicate member names are allowed (section 1) | Create team | Two member names differ only by case or surrounding whitespace | Name equality alone must not merge or reject distinct members | None | 201 |
| IDs are generated UUIDs (section 5) | Create team | Team is created | Assign a UUID to the team | None | 201 |
| IDs are generated UUIDs (section 5) | Create team | Initial members are created | Assign each member a unique UUID | None | 201 |
| Valid teams can be created (section 1; section 5) | Create team | Name is valid and unique; at least one valid member is supplied | Create team | None | 201 |
| Teams can be retrieved (section 5) | Get team | Team exists | Return team data | None | 200 |
| Teams can be listed (section 5) | List teams | Listing is requested | Return teams without required pagination | None | 200 |
| Members may be added (section 1) | Add member | Team exists and supplied name is valid | Add member with a generated UUID | None | 201 |
| Member name is required and nonblank (section 1) | Add member | Name is missing, null, empty, or whitespace-only | Reject request | Not specified | 400 |
| Member names contain only alphanumeric characters and whitespace (section 1) | Add member | Name contains disallowed characters | Reject request | Not specified | 400 |
| Member name length is 1-255 (opening table; section 1) | Add member | Name exceeds 255 characters | Reject request | Not specified | 400 |
| Duplicate member names are allowed (section 1) | Add member | Another member has the same name, including case/whitespace-equivalent names | Add a distinct member; identify by UUID | None | 201 |
| Names are stored exactly as submitted (section 6) | Add member | Valid name contains surrounding whitespace or mixed case | Preserve the submitted name | None | 201 |
| Members may be renamed (section 1) | Rename member | Member exists in the target team; new name is valid | Update member name | None | 200 |
| Member name is required and nonblank (section 1) | Rename member | New name is missing, null, empty, or whitespace-only | Reject request | Not specified | 400 |
| Member names contain only alphanumeric characters and whitespace (section 1) | Rename member | New name contains disallowed characters | Reject request | Not specified | 400 |
| Member name length is 1-255 (opening table; section 1) | Rename member | New name exceeds 255 characters | Reject request | Not specified | 400 |
| Duplicate member names are allowed (section 1) | Rename member | New name equals another member's name | Allow rename; names are not identifiers | None | 200 |
| Names are stored exactly as submitted (section 6) | Rename member | Valid new name contains surrounding whitespace or mixed case | Preserve the submitted name | None | 200 |
| A team must always have at least one member (section 1) | Add member | Membership changes | Team must still have at least one member | None | 201 |
| A team must always have at least one member (section 1) | Rename member | Membership changes | Renaming must not remove the team's members | None | 200 |
| Members may be removed (section 1) | Remove member | Removing the member leaves at least one member | Remove member | None | 204 |
| A team must always have at least one member (section 1) | Remove member | Target is the team's last member | Reject removal | Not specified | 400 |
| Name character repertoire (section 1) | Create team | A team/member name uses non-ASCII letters or digits, tabs, newlines, or other non-space whitespace | **DECIDED:** only ASCII letters, ASCII digits and the space character are allowed; reject anything else | Not specified | 400 |
| Name character repertoire (section 1) | Add member | Name uses non-ASCII letters or digits, tabs, newlines, or other non-space whitespace | **DECIDED:** only ASCII letters, ASCII digits and the space character are allowed; reject anything else | Not specified | 400 |
| Name character repertoire (section 1) | Rename member | Name uses non-ASCII letters or digits, tabs, newlines, or other non-space whitespace | **DECIDED:** only ASCII letters, ASCII digits and the space character are allowed; reject anything else | Not specified | 400 |
| Name length is measured after trimming (section 1; section 6) | Create team | Raw team/member name exceeds 255 characters but trimmed name does not | **DECIDED:** validate the trimmed length; accept | None | 201 |
| Name length is measured after trimming (section 1; section 6) | Add member | Raw name exceeds 255 characters but trimmed name does not | **DECIDED:** validate the trimmed length; accept | None | 201 |
| Name length is measured after trimming (section 1; section 6) | Rename member | Raw name exceeds 255 characters but trimmed name does not | **DECIDED:** validate the trimmed length; accept | None | 200 |
| Membership management needs no requester (brief: Technical constraints) | Add member | Request has no requester member ID, or requester is not on the team | **DECIDED:** no requester is required or checked; proceed | None | 201 |
| Membership management needs no requester (brief: Technical constraints) | Rename member | Request has no requester member ID, or requester is not on the team | **DECIDED:** no requester is required or checked; proceed | None | 200 |
| Membership management needs no requester (brief: Technical constraints) | Remove member | Request has no requester member ID, or requester is not on the team | **DECIDED:** no requester is required or checked; proceed | None | 204 |
| Members are addressed within their team | Rename member | Member exists, but belongs to another team than the route identifies | **DECIDED:** treat as a missing nested resource; reject with member-not-found message | Not specified | 404 |
| Members are addressed within their team | Remove member | Member exists, but belongs to another team than the route identifies | **DECIDED:** treat as a missing nested resource; reject with member-not-found message | Not specified | 404 |
| Removal keeps history | Remove member | Member has submitted feedback or action items | **DECIDED:** removal succeeds; existing feedback is kept with the submitter name recorded at submission | None | 204 |
| UUID identity overrides name matching (section 1; section 6) | Resolve member identity | A normalized name matches multiple distinct member IDs | **DECIDED:** members are always resolved by UUID; normalized names are used only for comparison such as team-name uniqueness, never to pick a member | None | Not applicable |

## Retrospectives

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
| --- | --- | --- | --- | --- | --- |
| Title is required and nonblank (opening table; section 2) | Create retrospective | Title is missing, null, empty, or whitespace-only | Reject request | Not specified | 400 |
| Title length is 1-255 (opening table) | Create retrospective | Title exceeds 255 characters | Reject request | Not specified | 400 |
| Date is generated as today (section 2) | Create retrospective | Client supplies no date | Generate today's calendar date, without stored time or timezone | None | 201 |
| Status defaults to OPEN (section 2) | Create retrospective | Status is omitted and team has no OPEN retrospective | Create retrospective with status OPEN | None | 201 |
| Valid explicit status may override default (section 2) | Create retrospective | Client supplies a valid status | Use supplied status, subject to the one-OPEN rule | None | 201 |
| Status is OPEN or CLOSED (brief: Retrospectives; section 2) | Create retrospective | Supplied status is outside OPEN/CLOSED | Reject invalid status | Not specified | 400 |
| A retrospective belongs to one team (brief: Retrospectives) | Create retrospective | Creation succeeds for the target team | Associate the retrospective with that team | None | 201 |
| Only one OPEN retrospective per team (section 2) | Create retrospective | Creating an OPEN retrospective when target team already has one | Reject with message `Only 1 retro can be open` | Not specified | 400 |
| One-OPEN restriction is per team (section 2) | Create retrospective | Another team has an OPEN retrospective, target team does not | Allow OPEN retrospective for target team | None | 201 |
| Closing permits a new OPEN retrospective (section 2) | Create retrospective | Target team's previous OPEN retrospective has been closed | Allow new OPEN retrospective | None | 201 |
| Duplicate closed retrospectives are allowed (section 6) | Create retrospective | Resulting CLOSED retrospective shares title/date with another CLOSED retrospective | Allow duplicate; do not enforce title/date uniqueness | None | 201 |
| Duplicates are allowed subject to the one-OPEN rule (section 6) | Create retrospective | New OPEN retrospective shares title/date with a CLOSED retrospective and team has no OPEN retrospective | Allow duplicate; title/date do not block creation | None | 201 |
| Retrospective IDs are generated UUIDs (section 5) | Create retrospective | Creation succeeds | Assign a generated UUID | None | 201 |
| Requester must be identified by member ID (section 1; section 2) | Create retrospective | Member ID is missing | Reject request | Not specified | 400 |
| Requester must belong to target team (section 2; section 5) | Create retrospective | Supplied member exists but belongs to a different team | Reject as non-member action | Not specified | 400 |
| Any team member may create a retrospective (brief: Retrospectives) | Create retrospective | Requester belongs to the team but has no special role | Allow creation if other rules pass; no privileged role is required | None | 201 |
| Valid retrospectives can be created (section 2; section 5) | Create retrospective | Valid title, existing team, authorized member, and permitted status | Create retrospective | None | 201 |
| Retrospectives can be retrieved (section 5) | Get retrospective | Retrospective exists | Return retrospective data | None | 200 |
| Retrospectives can be listed by team (section 5) | List team retrospectives | Team exists | Return its retrospectives without required pagination | None | 200 |
| OPEN retrospectives can be closed (section 2) | Close retrospective | Retrospective is OPEN and requester belongs to its team | Change status to CLOSED | None | 200 |
| Requester must be identified by member ID (section 1; section 2) | Close retrospective | Member ID is missing | Reject request | Not specified | 400 |
| Requester must belong to owning team (section 2; section 5) | Close retrospective | Supplied member exists but belongs to a different team | Reject as non-member action | Not specified | 400 |
| Any team member may close a retrospective (brief: Retrospectives) | Close retrospective | Requester belongs to the team but is not the creator and has no special role | Allow closure if retrospective is OPEN; no creator-only restriction | None | 200 |
| Closing twice is forbidden (section 2) | Close retrospective | Retrospective is already CLOSED | Reject with message `Retrospective already closed` | Not specified | 400 |
| Retrospective title is immutable (section 2) | Modify retrospective title | Attempt to change title after creation | Reject change; no editing route is specified | Not specified | 400 for rule violation; 405 if method unsupported |
| Retrospective date is immutable (section 2) | Modify retrospective date | Attempt to change date after creation | Reject change; no editing route is specified | Not specified | 400 for rule violation; 405 if method unsupported |
| Future dates are allowed (section 2) | Create retrospective | Client explicitly supplies a future date | **DECIDED:** the client may optionally supply the date as ISO yyyy-MM-dd; accept it | None | 201 |
| Optional supplied date (section 2) | Create retrospective | Client explicitly supplies a current or past date | **DECIDED:** accept it; any valid calendar date is allowed | None | 201 |
| Supplied date must be a valid calendar date | Create retrospective | Supplied date is malformed or not a real date | **DECIDED:** reject request | Not specified | 400 |
| Today is calculated in UTC (section 2) | Create retrospective | Calendar date differs between server and requester | **DECIDED:** use the current date in UTC | None | 201 |
| Null status means omitted (section 2) | Create retrospective | Status is explicitly null | **DECIDED:** treat as omitted; default to OPEN | None | 201 |
| One-OPEN rule counts OPEN retrospectives only (brief: Retrospectives; section 6) | Create retrospective | Team already has an OPEN retrospective and client requests CLOSED | **DECIDED:** allow; the rule restricts only the number of OPEN retrospectives | None | 201 |
| Closed retrospectives cannot be reopened (brief: Retrospectives) | Reopen retrospective | Client attempts CLOSED to OPEN | **DECIDED:** no reopen operation exists and status is not client-modifiable after creation; any attempt is rejected as a rule violation | Not specified | 400 |
| Closing route is PUT /api/retrospectives/{id}/close | Close retrospective | A binding route and HTTP method are needed | **DECIDED:** use PUT with the /api prefix, matching the implemented controller instead of section 5's POST suggestion; POST on this path is rejected as unsupported | Not specified | 200 for successful close; 405 for POST |

## Feedback

Action items are specialized feedback, so feedback rules also apply to them. Action-specific operations are expanded in the next section.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
| --- | --- | --- | --- | --- | --- |
| Feedback can be created while OPEN (section 3) | Create feedback | Retrospective is OPEN and request is otherwise valid | Create feedback | None | 201 |
| Feedback belongs to a retrospective (brief: Feedback items) | Create feedback | Creation succeeds for the target retrospective | Associate feedback with that retrospective | None | 201 |
| Closed feedback is read-only (section 3) | Create feedback | Retrospective is CLOSED | Reject addition | Not specified | 400 |
| Feedback can be read while OPEN (section 3) | List retrospective feedback | Retrospective is OPEN | Return its feedback | None | 200 |
| Closed feedback remains readable (section 3) | List retrospective feedback | Retrospective is CLOSED | Return its feedback without modification | None | 200 |
| Feedback can be modified while OPEN (section 3) | Modify feedback | Retrospective is OPEN and requested change is valid | Update feedback | None | 200 |
| Closed feedback cannot be modified (section 3) | Modify feedback | Retrospective is CLOSED | Reject modification | Not specified | 400 |
| Feedback can be deleted while OPEN (section 3) | Delete feedback | Retrospective is OPEN | Delete feedback | None | 204 |
| Closed feedback cannot be deleted (section 3) | Delete feedback | Retrospective is CLOSED | Reject deletion | Not specified | 400 |
| Content is required and nonblank (section 3) | Create feedback | Content is missing, null, empty, or whitespace-only | Reject request | Not specified | 400 |
| Feedback content maximum is 4000 (opening table; section 3) | Create feedback | Content is 256-4000 characters | **DECIDED:** 4000 applies to feedback; section 1's 255 applies to names | None | 201 |
| Feedback exceeds every documented maximum | Create feedback | Content exceeds 4000 characters | Reject request under either stated limit | Not specified | 400 |
| Submitter ID is required (section 3) | Create feedback | Submitter ID is missing | Reject request | Not specified | 400 |
| Submitter must belong to retrospective's team (section 3) | Create feedback | Submitter exists but is not a member of that team | Reject submission | Not specified | 400 |
| Feedback type is required (section 3) | Create feedback | Type is missing or null | Reject request | Not specified | 400 |
| Feedback types are WENT_WELL, NEEDS_IMPROVEMENT, ACTION_ITEM (brief: Feedback items) | Create feedback | Type is an unrecognized value other than the conflicting POSITIVE/NEGATIVE examples | Reject invalid type | Not specified | 400 |
| Non-action feedback types are defined by the brief (brief: Feedback items) | Create feedback | Type is WENT_WELL or NEEDS_IMPROVEMENT and other fields are valid | Accept the documented non-action type | None | 201 |
| The brief's feedback types are authoritative (brief: Feedback items) | Create feedback | Type is POSITIVE or NEGATIVE | **DECIDED:** section 3's names are illustrative only; reject, no aliases | Not specified | 400 |
| Feedback records the submitting member's name (brief: Feedback items; section 3) | Create feedback | Submission succeeds using the member ID required by section 3 | **DECIDED:** store the submitter's name as a snapshot at submission; later renames do not change it | None | 201 |
| Non-action feedback must not carry priority (section 4) | Create feedback | Non-action type includes priority | Reject request | Not specified | 400 |
| Non-action feedback must not carry completed (section 4) | Create feedback | Non-action type includes completed, including false | Reject request | Not specified | 400 |
| Duplicate feedback content is allowed (section 6) | Create feedback | Another feedback item has identical content | Allow duplicate | None | 201 |
| Feedback IDs are generated UUIDs (section 5) | Create feedback | Creation succeeds | Assign a generated UUID | None | 201 |
| Content remains nonblank (section 3) | Modify feedback | Supplied replacement content is null, empty, or whitespace-only | Reject invalid content | Not specified | 400 |
| Feedback content maximum is 4000 (opening table; section 3) | Modify feedback | Replacement content is 256-4000 characters | **DECIDED:** accept | None | 200 |
| Feedback exceeds every documented maximum | Modify feedback | Replacement content exceeds 4000 characters | Reject invalid content under either stated limit | Not specified | 400 |
| Feedback type is immutable (section 3) | Modify feedback | Attempt to change type, including conversion to/from ACTION_ITEM | Reject type change | Not specified | 400 |
| The brief's feedback types are authoritative (brief: Feedback items) | Modify feedback | An update supplies POSITIVE or NEGATIVE as the type | **DECIDED:** reject as an invalid type | Not specified | 400 |
| Non-action feedback must not carry priority (section 4) | Modify feedback | Non-action feedback update supplies priority | Reject request | Not specified | 400 |
| Non-action feedback must not carry completed (section 4) | Modify feedback | Non-action feedback update supplies completed | Reject request | Not specified | 400 |
| Duplicate feedback content is allowed (section 6) | Modify feedback | Replacement content equals another item's content | Allow duplicate content | None | 200 |
| Feedback retrieval is scoped to a retrospective (section 3) | List retrospective feedback | Existing retrospective ID is supplied | Return feedback for that retrospective, not a global list | None | 200 |
| No global feedback endpoint is required (section 3) | List global feedback | A global feedback list is requested | **DECIDED:** no such route exists; unmapped path | Not specified | 404 |
| Update requires content only (section 3) | Modify feedback | Content is omitted | **DECIDED:** content is required on update; reject | Not specified | 400 |
| Update requires content only (section 3) | Modify feedback | Type or submitterId is omitted | **DECIDED:** keep the existing values; accept | None | 200 |
| Feedback editing needs no requester (brief: Technical constraints) | Modify feedback | Requester is not the submitter or is not on the team | **DECIDED:** no requester is required or checked; any caller may edit | None | 200 |
| Feedback deletion needs no requester (brief: Technical constraints) | Delete feedback | Requester is not the submitter or is not on the team | **DECIDED:** no requester is required or checked; any caller may delete | None | 204 |
| Submitter is immutable | Modify feedback | Update supplies a submitterId different from the existing one | **DECIDED:** reject the change | Not specified | 400 |
| Null means absent (section 4) | Create feedback | Non-action feedback explicitly includes priority or completed as null | **DECIDED:** null is treated as absent; accept | None | 201 |
| Null means absent (section 4) | Modify feedback | Non-action feedback explicitly includes priority or completed as null | **DECIDED:** null is treated as absent; accept | None | 200 |

## Action Items

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
| --- | --- | --- | --- | --- | --- |
| Action item is specialized feedback (section 4) | Create action item | Feedback is created with type ACTION_ITEM | Represent it as feedback with action-specific priority and completion state | None | 201 |
| Action items belong to a retrospective (brief: Feedback items; Action items) | Create action item | Creation succeeds for the target retrospective | Associate the specialized feedback with that retrospective | None | 201 |
| Action items record the submitting member's name (brief: Feedback items; Action items; section 3) | Create action item | Submission succeeds using submitterId | **DECIDED:** store the submitter's name as a snapshot at submission; later renames do not change it | None | 201 |
| Action-item content length is 1-4000 (opening table) | Create action item | Content is missing, null, empty, or whitespace-only | Reject request; nonblank feedback rule also applies | Not specified | 400 |
| Feedback and action-item content maximum is 4000 (opening table) | Create action item | Content is 256-4000 characters | **DECIDED:** accept | None | 201 |
| Action-item content exceeds every documented maximum | Create action item | Content exceeds 4000 characters | Reject request | Not specified | 400 |
| Submitter ID is required for feedback (section 3; section 4) | Create action item | Submitter ID is missing | Reject request | Not specified | 400 |
| Submitter must belong to retrospective's team (section 3; section 4) | Create action item | Submitter exists but is not a member of that team | Reject submission | Not specified | 400 |
| Action items require priority (section 4) | Create action item | Priority is missing or null | Reject request | Not specified | 400 |
| Completed defaults to false (section 4) | Create action item | Completed is omitted | **DECIDED:** default to false; accept | None | 201 |
| Default initial completion is false (section 4) | Create action item | Default completion state is applied | Initialize as incomplete; whether omission is allowed remains unresolved above | None | 201 if creation accepted |
| Action items start incomplete (section 4) | Create action item | Client supplies completed=true | **DECIDED:** initial state must be false; reject | Not specified | 400 |
| Null means absent (section 4) | Create action item | Client supplies completed=null | **DECIDED:** treat as omitted; default to false | None | 201 |
| Priority is LOW, MEDIUM or HIGH (brief: Action items; Technical constraints) | Create action item | Supplied priority is outside LOW/MEDIUM/HIGH | Reject invalid priority with a meaningful message | Not specified | 400 |
| Completion is boolean (brief: Action items; Technical constraints) | Create action item | Supplied completed value cannot represent a boolean | Reject invalid input; accepted textual/numeric coercions are not specified | Not specified | 400 |
| Action items may be added while OPEN (section 3; section 4) | Create action item | Retrospective is OPEN and fields are valid | Create action item | None | 201 |
| Closed feedback is read-only (section 3; section 4) | Create action item | Retrospective is CLOSED | Reject addition | Not specified | 400 |
| Duplicate feedback content is allowed (section 6) | Create action item | Another feedback/action item has identical content | Allow duplicate content | None | 201 |
| Feedback IDs are generated UUIDs (section 5) | Create action item | Creation succeeds | Assign a generated UUID as specialized feedback | None | 201 |
| Action-item content may be edited while OPEN (section 4) | Modify action-item content | Retrospective is OPEN and replacement content is valid | Update content without resetting completion | None | 200 |
| Action-item content remains nonblank (opening table; section 3; section 4) | Modify action-item content | Replacement content is null, empty, or whitespace-only | Reject request | Not specified | 400 |
| Feedback and action-item content maximum is 4000 (opening table) | Modify action-item content | Replacement content is 256-4000 characters | **DECIDED:** accept | None | 200 |
| Action-item content exceeds every documented maximum | Modify action-item content | Replacement content exceeds 4000 characters | Reject request | Not specified | 400 |
| Closed feedback cannot be modified (section 3; section 4) | Modify action-item content | Retrospective is CLOSED | Reject modification | Not specified | 400 |
| Completed action items may still have content edited while OPEN (section 4) | Modify action-item content | Item is already completed and retrospective is OPEN | Allow valid edit; keep completed=true | None | 200 |
| Duplicate feedback content is allowed (section 6) | Modify action-item content | Replacement content equals another item's content | Allow duplicate content | None | 200 |
| Action-item priority may be edited while OPEN (section 4) | Modify action-item priority | Retrospective is OPEN and supplied priority is valid | Update priority without resetting completion | None | 200 |
| Closed feedback cannot be modified (section 3; section 4) | Modify action-item priority | Retrospective is CLOSED | Reject modification | Not specified | 400 |
| Completed action items may still have priority edited while OPEN (section 4) | Modify action-item priority | Item is already completed and retrospective is OPEN | Allow valid edit; keep completed=true | None | 200 |
| Action items must retain priority (section 4) | Modify action-item priority | Update removes or nulls priority | Reject request | Not specified | 400 |
| Priority is LOW, MEDIUM or HIGH (brief: Action items; Technical constraints) | Modify action-item priority | Supplied priority is outside LOW/MEDIUM/HIGH | Reject invalid priority with a meaningful message | Not specified | 400 |
| Feedback type is immutable (section 3; section 4) | Modify action-item type | Attempt to change ACTION_ITEM to another type | Reject conversion | Not specified | 400 |
| Closed feedback cannot be modified (section 3; section 4) | Modify action-item type | Retrospective is CLOSED | Reject modification; immutability of type also independently forbids conversion | Not specified | 400 |
| Action items may become complete while OPEN (section 4) | Complete action item | Item is incomplete and retrospective is OPEN | Change completed from false to true | None | 200 |
| Closed action items cannot be completed (section 4) | Complete action item | Retrospective is CLOSED | Reject modification | Not specified | 400 |
| Completion is idempotent (section 4) | Complete action item | Item is already completed and retrospective is OPEN | **DECIDED:** succeed with no change | None | 200 |
| Completed action items cannot be uncompleted (section 4) | Uncomplete action item | Attempt true to false while retrospective is OPEN | Reject with message `Action item cannot be uncompleted` | Not specified | 400 |
| Closed feedback cannot be modified (section 3; section 4) | Uncomplete action item | Retrospective is CLOSED | **DECIDED:** the closed-retrospective check comes first; reject as a closed-retrospective modification | Not specified | 400 |
| Completion cannot be reset through content edits (section 4) | Modify action-item content | Edit also attempts completed=true to completed=false | Reject uncompletion with message `Action item cannot be uncompleted` | Not specified | 400 |
| Completion cannot be reset through priority edits (section 4) | Modify action-item priority | Edit also attempts completed=true to completed=false | Reject uncompletion with message `Action item cannot be uncompleted` | Not specified | 400 |
| Completion is boolean (brief: Action items; Technical constraints) | Modify action-item content | Edit also supplies a completed value that cannot represent a boolean | Reject invalid input; accepted coercions are not specified | Not specified | 400 |
| Completion is boolean (brief: Action items; Technical constraints) | Modify action-item priority | Edit also supplies a completed value that cannot represent a boolean | Reject invalid input; accepted coercions are not specified | Not specified | 400 |
| No dedicated uncomplete operation (section 4) | Uncomplete action item | Update supplies completed=false for an item that is already incomplete, retrospective OPEN | **DECIDED:** no separate route exists; the update is a no-op success | None | 200 |
| Action items may be deleted while OPEN (section 4) | Delete action item | Retrospective is OPEN, whether item is complete or incomplete | Delete item | None | 204 |
| Closed feedback cannot be deleted (section 3; section 4) | Delete action item | Retrospective is CLOSED | Reject deletion | Not specified | 400 |
| Action-item editing needs no requester (brief: Technical constraints) | Modify action-item content | Requester is not the submitter or is not on the team | **DECIDED:** no requester is required or checked | None | 200 |
| Action-item editing needs no requester (brief: Technical constraints) | Modify action-item priority | Requester is not the submitter or is not on the team | **DECIDED:** no requester is required or checked | None | 200 |
| Completion needs no requester (brief: Technical constraints) | Complete action item | Requester is not the submitter or is not on the team | **DECIDED:** no requester is required or checked | None | 200 |
| Deletion needs no requester (brief: Technical constraints) | Delete action item | Requester is not the submitter or is not on the team | **DECIDED:** no requester is required or checked | None | 204 |
| Submitter is immutable | Modify action-item submitter | Update supplies a submitterId different from the existing one | **DECIDED:** reject the change | Not specified | 400 |
| Closed feedback is wholly read-only (section 3; section 4) | Modify action-item submitter | Retrospective is CLOSED | Reject any attempted reassignment regardless of the unresolved OPEN-state policy | Not specified | 400 |

## Action-Item Queries

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
| --- | --- | --- | --- | --- | --- |
| Action-item query spans all retrospectives (section 4) | List action items | No filters supplied | Return action items across all retrospectives, not just one team or OPEN retrospectives | None | 200 |
| Completion filtering is supported (section 4) | Filter action items by completion | completed=true | Return only completed action items | None | 200 |
| Completion filtering is supported (section 4) | Filter action items by completion | completed=false | Return only incomplete action items | None | 200 |
| Priority filtering is supported (section 4) | Filter action items by priority | A valid priority is supplied | Return only matching-priority action items | None | 200 |
| Priority filter uses LOW, MEDIUM or HIGH (brief: Action items; Technical constraints) | Filter action items by priority | Value is outside the documented priority set, excluding unresolved case/blank syntax | Reject invalid priority with a meaningful message | Not specified | 400 |
| Filters can be combined (section 4) | Filter action items by priority and completion | Both filters are supplied | Return items matching both filters | None | 200 |
| OPEN retrospectives sort first (section 4) | List action items | Results span OPEN and CLOSED retrospectives | Place items from OPEN retrospectives before items from CLOSED retrospectives | None | 200 |
| OPEN retrospectives sort first (section 4) | Filter action items by completion | Matching items span OPEN and CLOSED retrospectives | Place OPEN-retrospective items first | None | 200 |
| OPEN retrospectives sort first (section 4) | Filter action items by priority | Matching items span OPEN and CLOSED retrospectives | Place OPEN-retrospective items first | None | 200 |
| OPEN retrospectives sort first (section 4) | Filter action items by priority and completion | Matching items span OPEN and CLOSED retrospectives | Place OPEN-retrospective items first | None | 200 |
| Priority is the second sort key (section 4) | List action items | Items have equal retrospective-status rank | Order HIGH, then MEDIUM, then LOW | None | 200 |
| Priority is the second sort key (section 4) | Filter action items by completion | Matching items have equal retrospective-status rank | Order HIGH, then MEDIUM, then LOW | None | 200 |
| Priority is the second sort key (section 4) | Filter action items by priority | All matching items have the selected priority | Retain priority key; it cannot distinguish these rows | None | 200 |
| Priority is the second sort key (section 4) | Filter action items by priority and completion | All matching items have the selected priority | Retain priority key; it cannot distinguish these rows | None | 200 |
| Creation order is the final sort key (section 4) | List action items | Retrospective-status and priority ranks tie | **DECIDED:** oldest first; ties on creation time broken by ID ascending | None | 200 |
| Creation order is the final sort key (section 4) | Filter action items by completion | Retrospective-status and priority ranks tie | **DECIDED:** oldest first; ties on creation time broken by ID ascending | None | 200 |
| Creation order is the final sort key (section 4) | Filter action items by priority | Retrospective-status ranks tie | **DECIDED:** oldest first; ties on creation time broken by ID ascending | None | 200 |
| Creation order is the final sort key (section 4) | Filter action items by priority and completion | Retrospective-status ranks tie | **DECIDED:** oldest first; ties on creation time broken by ID ascending | None | 200 |
| Pagination is not required (section 4; section 5) | List action items | Results are requested | No pagination requirement | None | 200 |
| Pagination is not required (section 4; section 5) | Filter action items by completion | Results are requested | No pagination requirement | None | 200 |
| Pagination is not required (section 4; section 5) | Filter action items by priority | Results are requested | No pagination requirement | None | 200 |
| Pagination is not required (section 4; section 5) | Filter action items by priority and completion | Results are requested | No pagination requirement | None | 200 |
| Completion filter accepts true or false only | Filter action items by completion | Value is not true or false (case-insensitive), including blank, 1, 0, yes or no | **DECIDED:** reject; no other coercions | Not specified | 400 |
| Priority filter is case-insensitive and nonblank | Filter action items by priority | Priority is a case-varied spelling such as high | **DECIDED:** accept | None | 200 |
| Priority filter is case-insensitive and nonblank | Filter action items by priority | Priority parameter is present but blank | **DECIDED:** reject | Not specified | 400 |
| Any invalid filter rejects the request | Filter action items by priority and completion | One or both filter values are invalid | **DECIDED:** reject; the message reports the priority error before the completion error | Not specified | 400 |

## Missing Resources and Identifier Validation

These rows separate absent resources from existing resources that violate a business rule. A malformed UUID is invalid input, not evidence that a resource is absent.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
| --- | --- | --- | --- | --- | --- |
| Missing team is not found (section 5) | Get team | Team UUID is valid but absent | Reject with team-not-found message | Not specified | 404 |
| Missing team is not found (section 5) | Add member | Target team is absent | Reject with team-not-found message | Not specified | 404 |
| Missing team is not found (section 5) | Rename member | Target team is absent | Reject with team-not-found message | Not specified | 404 |
| Missing member is not found (section 5) | Rename member | Target member is absent | Reject with member-not-found message | Not specified | 404 |
| Missing team is not found (section 5) | Remove member | Target team is absent | Reject with team-not-found message | Not specified | 404 |
| Missing member is not found (section 5) | Remove member | Target member is absent | Reject with member-not-found message | Not specified | 404 |
| Missing team is not found (section 5) | Create retrospective | Target team is absent | Reject with team-not-found message | Not specified | 404 |
| Missing member is not found (section 5) | Create retrospective | Supplied requester member UUID does not exist | Reject with member-not-found message | Not specified | 404 |
| Missing team is not found (section 5) | List team retrospectives | Target team is absent | Reject with team-not-found message | Not specified | 404 |
| Missing retrospective is not found (section 5) | Get retrospective | Retrospective is absent | Reject with message `Retrospective not found` | Not specified | 404 |
| Missing retrospective is not found (section 5) | Close retrospective | Retrospective is absent | Reject with message `Retrospective not found` | Not specified | 404 |
| Missing member is not found (section 5) | Close retrospective | Supplied requester member UUID does not exist | Reject with member-not-found message | Not specified | 404 |
| Missing retrospective is not found (section 5) | Create feedback | Parent retrospective is absent | Reject with message `Retrospective not found` | Not specified | 404 |
| Missing member is not found (section 5) | Create feedback | Supplied submitter UUID does not exist | Reject with member-not-found message | Not specified | 404 |
| Missing retrospective is not found (section 5) | List retrospective feedback | Parent retrospective is absent | Reject with message `Retrospective not found` | Not specified | 404 |
| Missing feedback is not found (section 5) | Modify feedback | Feedback item is absent | Reject with feedback-not-found message | Not specified | 404 |
| Missing feedback is not found (section 5) | Delete feedback | Feedback item is absent | Reject with feedback-not-found message | Not specified | 404 |
| Missing retrospective is not found (section 5) | Create action item | Parent retrospective is absent | Reject with message `Retrospective not found` | Not specified | 404 |
| Missing member is not found (section 5) | Create action item | Supplied submitter UUID does not exist | Reject with member-not-found message | Not specified | 404 |
| Missing feedback is not found (section 5) | Modify action-item content | Action item is absent | Reject with a resource-not-found message | Not specified | 404 |
| Missing feedback is not found (section 5) | Modify action-item priority | Action item is absent | Reject with a resource-not-found message | Not specified | 404 |
| Missing feedback is not found (section 5) | Modify action-item type | Action item is absent | Reject with a resource-not-found message | Not specified | 404 |
| Missing feedback is not found (section 5) | Modify action-item submitter | Action item is absent | Reject with a resource-not-found message | Not specified | 404 |
| Missing feedback is not found (section 5) | Complete action item | Action item is absent | Reject with a resource-not-found message | Not specified | 404 |
| Missing feedback is not found (section 5) | Uncomplete action item | Update with completed=false targets an absent action item | **DECIDED:** no dedicated route; the update route rejects with a resource-not-found message | Not specified | 404 |
| Missing feedback is not found (section 5) | Delete action item | Action item is absent | Reject with a resource-not-found message | Not specified | 404 |
| Identifiers are UUIDs (section 5) | Get team | Team ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Add member | Team ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Rename member | Team or member ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Remove member | Team or member ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Create retrospective | Team or requester member ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Get retrospective | Retrospective ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | List team retrospectives | Team ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Close retrospective | Retrospective or requester member ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Create feedback | Retrospective or submitter ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | List retrospective feedback | Retrospective ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Modify feedback | Feedback ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Delete feedback | Feedback ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Create action item | Retrospective or submitter ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Modify action-item content | Action-item ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Modify action-item priority | Action-item ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Modify action-item type | Action-item ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Modify action-item submitter | Action-item ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Complete action item | Action-item ID is malformed | Reject invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Uncomplete action item | Action-item ID is malformed in an update with completed=false | **DECIDED:** no dedicated route; the update route rejects invalid input | Not specified | 400 |
| Identifiers are UUIDs (section 5) | Delete action item | Action-item ID is malformed | Reject invalid input | Not specified | 400 |

## Response and Scope Rules

These are API/operational requirements rather than domain invariants, included so the documented decisions are not silently omitted. The error-response rule applies independently to each error row above: return JSON with a `message` field. Exact messages are only fixed where quoted in the matrix; exception classes are never specified.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
| --- | --- | --- | --- | --- | --- |
| Related objects are excluded from responses (section 5) | Create team | Creation succeeds | **DECIDED:** return the team id and name plus members as objects with id and name; no retrospectives | None | 201 |
| Related objects are excluded from responses (section 5) | Get team | Team exists | **DECIDED:** same shape as create: team id and name plus members with id and name; no retrospectives | None | 200 |
| Related objects are excluded from responses (section 5) | List teams | Listing succeeds | Return team data without related-object expansion | None | 200 |
| Related objects are excluded from responses (section 5) | Add member | Creation succeeds | Do not embed related team/other objects | None | 201 |
| Related objects are excluded from responses (section 5) | Rename member | Update succeeds | Do not embed related team/other objects | None | 200 |
| Related objects are excluded from responses (section 5) | Create retrospective | Creation succeeds | Do not embed related team or feedback objects | None | 201 |
| Related objects are excluded from responses (section 5) | Get retrospective | Retrospective exists | Return retrospective data only; retrieve feedback separately | None | 200 |
| Related objects are excluded from responses (section 5) | List team retrospectives | Listing succeeds | Do not embed related team or feedback objects | None | 200 |
| Related objects are excluded from responses (section 5) | Close retrospective | Update succeeds | Do not embed related team or feedback objects | None | 200 |
| Related objects are excluded from responses (section 5) | Create feedback | Creation succeeds | Do not embed related retrospective or member objects | None | 201 |
| Related objects are excluded from responses (section 5) | List retrospective feedback | Listing succeeds | Do not embed related retrospective or member objects | None | 200 |
| Related objects are excluded from responses (section 5) | Modify feedback | Update succeeds | Do not embed related retrospective or member objects | None | 200 |
| Related objects are excluded from responses (section 5) | Create action item | Creation succeeds | Do not embed related retrospective or member objects | None | 201 |
| Related objects are excluded from responses (section 5) | Modify action-item content | Update succeeds | Do not embed related retrospective or member objects | None | 200 |
| Related objects are excluded from responses (section 5) | Modify action-item priority | Update succeeds | Do not embed related retrospective or member objects | None | 200 |
| Related objects are excluded from responses (section 5) | Complete action item | Update succeeds | Do not embed related retrospective or member objects | None | 200 |
| Related objects are excluded from responses (section 5) | List action items | Query succeeds | Do not embed related retrospective or member objects | None | 200 |
| Related objects are excluded from responses (section 5) | Filter action items by completion | Query succeeds | Do not embed related retrospective or member objects | None | 200 |
| Related objects are excluded from responses (section 5) | Filter action items by priority | Query succeeds | Do not embed related retrospective or member objects | None | 200 |
| Related objects are excluded from responses (section 5) | Filter action items by priority and completion | Query succeeds | Do not embed related retrospective or member objects | None | 200 |
| Pagination is outside scope (section 5) | List teams | Listing succeeds | No pagination requirement | None | 200 |
| Pagination is outside scope (section 5) | List team retrospectives | Listing succeeds | No pagination requirement | None | 200 |
| Pagination is outside scope (section 5) | List retrospective feedback | Listing succeeds | No pagination requirement | None | 200 |
| Empty lists are empty arrays | List teams | There are no teams | **DECIDED:** return an empty JSON array | None | 200 |
| Empty lists are empty arrays | List team retrospectives | Team exists but has no retrospectives | **DECIDED:** return an empty JSON array | None | 200 |
| Empty lists are empty arrays | List retrospective feedback | Retrospective exists but has no feedback | **DECIDED:** return an empty JSON array | None | 200 |
| Empty lists are empty arrays | List action items | There are no action items | **DECIDED:** return an empty JSON array | None | 200 |
| Empty lists are empty arrays | Filter action items by completion | No items match | **DECIDED:** return an empty JSON array | None | 200 |
| Empty lists are empty arrays | Filter action items by priority | No items match | **DECIDED:** return an empty JSON array | None | 200 |
| Empty lists are empty arrays | Filter action items by priority and completion | No items match both filters | **DECIDED:** return an empty JSON array | None | 200 |
| Creation audit history is not required (section 6) | Create retrospective | Creation succeeds | No requirement to record who created it | None | 201 |
| Closure audit history is not required (section 6) | Close retrospective | Closing succeeds | No requirement to record who closed it or when | None | 200 |
| Completion audit history is not required (section 6) | Complete action item | Completion succeeds | No requirement to record who completed it or when | None | 200 |
| Persistence across restarts is not required (section 6) | Restart application | In-memory H2 data existed before restart | Data need not survive restart | None | Not applicable |
| Unsupported methods use 405 (section 5) | Invoke unsupported HTTP method | Path exists but method is not supported | Reject method; this is not a domain operation | Not specified | 405 |

## Trusted Access Without Authentication

The brief's Teams and Technical constraints sections explicitly require no authentication or security layer. The following rows assume valid input and all applicable domain rules are satisfied. They do not waive requester/submitter membership validation or resolve the membership-policy gaps flagged above.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
| --- | --- | --- | --- | --- | --- |
| No authentication is required (brief: Technical constraints) | Create team | Trusted caller supplies no login credentials or token | Do not require authentication | None | 201 |
| No authentication is required (brief: Technical constraints) | Get team | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | List teams | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Add member | Trusted caller supplies no login credentials or token | Do not require authentication | None | 201 |
| No authentication is required (brief: Technical constraints) | Rename member | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Remove member | Trusted caller supplies no login credentials or token | Do not require authentication | None | 204 |
| No authentication is required (brief: Technical constraints) | Create retrospective | Caller supplies member ID but no login credentials or token | Validate team membership without requiring authentication | None | 201 |
| No authentication is required (brief: Technical constraints) | Get retrospective | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | List team retrospectives | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Close retrospective | Caller supplies member ID but no login credentials or token | Validate team membership without requiring authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Create feedback | Caller supplies submitter ID but no login credentials or token | Validate submitter membership without requiring authentication | None | 201 |
| No authentication is required (brief: Technical constraints) | List retrospective feedback | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Modify feedback | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Delete feedback | Trusted caller supplies no login credentials or token | Do not require authentication | None | 204 |
| No authentication is required (brief: Technical constraints) | Create action item | Caller supplies submitter ID but no login credentials or token | Validate submitter membership without requiring authentication | None | 201 |
| No authentication is required (brief: Technical constraints) | Modify action-item content | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Modify action-item priority | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Complete action item | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Delete action item | Trusted caller supplies no login credentials or token | Do not require authentication | None | 204 |
| No authentication is required (brief: Technical constraints) | List action items | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Filter action items by completion | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Filter action items by priority | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |
| No authentication is required (brief: Technical constraints) | Filter action items by priority and completion | Trusted caller supplies no login credentials or token | Do not require authentication | None | 200 |

## Cross-Cutting Decisions

**DECIDED:**

- Error precedence when several rules fail: malformed input (400), then missing resource (404), then business-rule violation (400); a closed-retrospective violation is checked before any other business rule.
- Reads need no member ID and no membership validation.
- Request fields: null is treated as absent, unknown fields are ignored, wrong JSON types are rejected with 400.
- Client-supplied IDs, and other server-generated fields in a request body, are ignored.
- An action-specific operation on feedback that is not an ACTION_ITEM is treated as not found (404).
- Team, team-retrospective and retrospective-feedback lists are ordered by creation, oldest first, ties by ID ascending.
- Member removal physically deletes the member; historical feedback keeps the submitter name recorded at submission.
- Routes: section 5 paths with the `/api` prefix, closing retrospectives via PUT, no dedicated uncomplete route.
- Feedback types are exactly WENT_WELL, NEEDS_IMPROVEMENT and ACTION_ITEM; no aliases.

**Still open:**

- Java exception class names for input validation, business-rule violations and missing resources; every rejection row leaves them unspecified.
