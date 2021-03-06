/**
 * Copyright (C) 2014 Kaj Magnus Lindberg (born 1979)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/// <reference path="../../shared/plain-old-javascript.d.ts" />
/// <reference path="Flag.ts" />

//------------------------------------------------------------------------------
   module debiki2.admin.moderation {
//------------------------------------------------------------------------------


export class Post {

  static TitleId = 0;
  static BodyId = 1;
  static ConfigPostId = 65503;

  public approveBtnText = '';
  public inlineMessage = '';

  public hideViewSuggsLink = true;
  public hideDeleteBtn = true;
  public hideRejectEditsBtn = true;


  constructor(
    public id: number,
    public type: string,
    public pageId: string,
    public pageName: string,
    public userId: string,
    public userDisplayName: string,
    public status: string,
    public unapprovedText: string,
    public approvedText: string,
    public createdAt: Date,
    public numHandledFlags: number,
    public numPendingFlags: number,
    public numPendingEditSuggestions: number,
    public pendingFlags: Flag[]) {

    if (this.numHandledFlags == null)
      this.numHandledFlags = 0;

    if (this.numPendingFlags == null)
      this.numPendingFlags = 0;

    if (this.numPendingEditSuggestions == null)
      this.numPendingEditSuggestions = 0;

    switch (this.status) {
      case 'Deleted':
      case 'Hidden':
        this.hideDeleteBtn = true;
        break;
      case 'NewPrelApproved':
      case 'EditsPrelApproved':
        this.approveBtnText = 'Okay';
        this.hideDeleteBtn = false;
        break;
      case 'New':
        this.approveBtnText = 'Approve';
        this.hideDeleteBtn = false;
        break;
      case 'NewEdits':
        this.approveBtnText = 'Approve';
        this.hideRejectEditsBtn = false;
        break;
      default:
        this.hideViewSuggsLink = this.numPendingEditSuggestions == 0;
        break;
    }
  }


  public static fromJson(json: any) {
    var pendingFlags: Flag[] = [];
    if (json.pendingFlags) {
      pendingFlags = json.pendingFlags.map(flagJson => {
        return Flag.fromJson(flagJson);
      });
    }

    return new Post(
      json.id,
      json.type,
      json.pageId,
      json.pageName,
      json.userId,
      json.userDisplayName,
      json.status,
      json.unapprovedText,
      json.approvedText,
      json.createdAt,
      json.numHandledFlags,
      json.numPendingFlags,
      json.numPendingEditSuggestions,
      pendingFlags);
  }


  public get hideNewFlagsLink() { return !this.numPendingFlags; }
  public get hideOldFlagsLink() { return !this.numHandledFlags; }
  public get prettyFlags() { return 'TODO(prettyFlags)'; }


  public get url(): string {
    return '/-'+ this.pageId +'#post-'+ this.id;
  }

  public get pagePath(): string {
    return 'TOOD';
  }

  public get description(): string {
    var what;
    switch (this.id) {
      case Post.TitleId: what = 'Page title'; break;
      case Post.BodyId: what = 'Page'; break;
      case Post.ConfigPostId: what = 'Page config'; break;
      default: what = 'Comment'; break;
    }
    var text;
    switch (this.status) {
      case 'New': text = 'New ' + what; break; // COULD to lowercase
      case 'NewPrelApproved': text = 'New '+ what +', preliminarily approved'; break; // COULD lowercase
      case 'Approved': text = what; break;
      case 'Rejected': text = what +', rejected'; break;
      case 'Deleted': text = what +', deleted'; break;
      case 'Hidden': text = what +', hidden'; break;
      case 'EditsRejected': text = what +', edits rejected'; break;
      case 'NewEdits': text = what +', edited'; break;
      case 'EditsPrelApproved': text = what +', edits prel. approved'; break;
      default: text = what +', '+ status; break;
    }
    return text;
  }


  public get textOrDiffSafeHtml(): string {
    switch (this.status) {
      case 'New':
        return escapeHtml(this.unapprovedText);
      case 'NewPrelApproved':
      case 'Approved':
        return escapeHtml(this.approvedText);
      case 'Rejected':
      case 'Deleted':
      case 'Hidden':
        // Sometimes `unapprovedText` is undefined, nevertheless the post was rejected.
        return escapeHtml(this.unapprovedText || this.approvedText);
      case 'EditsRejected':
      case 'EditsPrelApproved':
      case 'NewEdits':
        return debiki.internal.makeHtmlDiff(this.approvedText, this.unapprovedText);
      default:
        debiki.v0.util.die('DwE38RUJ0');
    }
  }


  public clearFlags() {
    this.numHandledFlags += this.numPendingFlags;
    this.numPendingFlags = 0;
    this.pendingFlags = [];
  }


  public rejectEdits() {
    this.status = 'EditsRejected';
    this.approveBtnText = null;
    this.hideRejectEditsBtn = true;
  }
}


// COULD move to some debiki-common.js or debiki-utils.js?
function escapeHtml(html: string) {
  return html
   .replace(/&/g, "&amp;")
   .replace(/</g, "&lt;")
   .replace(/>/g, "&gt;");
   /// Could also replace ' and " if needs to escape attribute value.
}


//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=tcqwn list
