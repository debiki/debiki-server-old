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

/// <reference path="SettingsTarget.ts" />

//------------------------------------------------------------------------------
   module debiki2.admin.model {
//------------------------------------------------------------------------------


export class SpecialContent {

  public newText: string;


  constructor(
      public rootPageId: string,
      public contentId: string,
      public defaultText: string,
      public currentText: string) {
    if (!this.currentText) {
      this.currentText = this.defaultText;
    }
    this.newText = this.currentText;
  }


  public static fromJsonMap(json: any) {
    return new SpecialContent(
        json['rootPageId'],
        json['contentId'],
        json['defaultText'],
        json['anyCustomText']);
  }


  public toJson(): string {
    var map = {
      'rootPageId': this.rootPageId,
      'contentId': this.contentId,
      'defaultText': this.defaultText,
      'useDefaultText': this.newText == this.defaultText
    };

    if (this.newText != this.defaultText)
      map['anyCustomText'] = this.newText;

    return JSON.stringify(map);
  }
}


//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=tcqwn list
