/*
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

/// <reference path="../../typedefs/react/react.d.ts" />
/// <reference path="../dashbar/dashbar.ts" />
/// <reference path="../sidebar/sidebar.ts" />
/// <reference path="comments-toolbar.ts" />
/// <reference path="name-login-btns.ts" />
/// <reference path="../users/users-page.ts" />

//------------------------------------------------------------------------------
   module debiki2.reactelements {
//------------------------------------------------------------------------------

var ReactRouter = window['ReactRouter'];

export var sidebar;

export function initAllReactRoots() {
  var dashbarElem = document.getElementById('dw-dashbar');
  if (dashbarElem)
    React.render(debiki2.dashbar.Dashbar({}), dashbarElem);

  var sidebarElem = document.getElementById('dw-any-sidebar');
  if (sidebarElem) {
    sidebar = React.render(debiki2.sidebar.Sidebar({}), sidebarElem);
  }

  var commentsToolbarElem = document.getElementById('dw-comments-toolbar');
  if (commentsToolbarElem)
    React.render(CommentsToolbar({}), commentsToolbarElem);

  var nameLoginBtnsElem = document.getElementById('dw-name-login-btns');
  if (nameLoginBtnsElem)
    React.render(NameLoginBtns({}), nameLoginBtnsElem);

  var userPageElem = document.getElementById('dw-user-page');
  if (userPageElem) {
    ReactRouter.run(debiki2.users.routes(), (Handler) => {
      React.render(Handler({}), userPageElem);
    });
  }
}


//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=tcqwn list
