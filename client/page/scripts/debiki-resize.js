/* Makes threads resizable.
 * Copyright (C) 2010-2012 Kaj Magnus Lindberg (born 1979)
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


var d = { i: debiki.internal, u: debiki.v0.util };
var $ = d.i.$;


d.i.makeThreadResizableForPost = function(post) {
  var $thread = $(post).dwCheckIs('.dw-p').closest('.dw-t');

  // Make replies to the root thread resizable horizontally. (Takes
  // perhaps 100 ms on my 6 core 2.8 GHz AMD, 24 depth-1 reply columns.)
  // (But skip inline replies; they expand eastwards regardless.)
  // $makeThreadEastResizable must be called before $makePostResizable (not in
  // use though!), or $makeThreadEastResizable has no effect. No idea
  // why -- my guess is some jQuery code does something similar to
  // `$.find(..)', and finds the wrong resizable stuff,
  // if the *inner* tag is made resizable before the *outer* tag.
  //
  // However for touch devises, don't enable resizing of posts: it doesn't
  // work, and the resize handles steal touch events from buttons nearby.
  if (!Modernizr.touch)
    $thread.filter(function() {
      var $i = $(this);
      return !$i.is('.dw-i-t') && $i.parent().closest('.dw-t').is('.dw-hor');
    }).each($makeThreadEastResizable);
};


// Makes [threads layed out vertically] horizontally resizable.
function $makeThreadEastResizable() {
  $(this).resizable({
    resize: function() {
      d.i.SVG.$drawParentsAndTree.apply(this);
    },
    handles: 'e',
    stop: function(event, ui) {
      // jQuery has added `height: ...' to the thread's style attribute.
      // Unless removed, the therad won't resize itself when child
      // threads are opened/closed.
      $(this).css('height', '');
    }
  });
};



/**
 * Avoids posts that are almost as wide as the mobile phone's screen,
 * because since Debiki's scrolling is 2D it's otherwise annoyingly easy to
 * scroll the post's left/right edge off screen. — Other websites usually
 * don't have horizontal scrolling at all, so for them it's not possible to
 * scroll the text edge off screen.
 */
d.i.setDynamicMaxPostWidth = function() {
  d.u.zoomListeners.push(updateMaxPostWidth);
  updateMaxPostWidth();
};



function updateMaxPostWidth() {
  var winWidth = $(window).width();
  if (winWidth >= 768)
    return;

  var maxWidth = winWidth * 0.85 - 50;
  $('.dw-p').css('max-width', maxWidth);

  // Alternatively, could add a max width CSS rule and edit that rule itself,
  // see e.g.:
  //  http://stackoverflow.com/questions/730048/how-to-change-remove-css-classes-definitions-at-runtime
  //  http://www.hunlock.com/blogs/Totally_Pwn_CSS_with_Javascript
  //  http://stackoverflow.com/questions/9153718/change-the-style-of-an-entire-css-class-using-javascript
};



// vim: fdm=marker et ts=2 sw=2 tw=80 fo=tcqwn list
