/* Swipe gestures for mobile phones and browsers.
 * Copyright (C) 2013 Kaj Magnus Lindberg (born 1979)
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

/* This AngularJS directive shows a minimap in which the article and all
 * comments are shown. The boundaries of the viewport are outlined too.
 * If you click and drag in the minimap, you'll scroll the viewport.
 *
 * Regrettably this code probably doesn't totally followed AngularJS best
 * practises, for example, it uses jQuery(window) rather than getting
 * the window as a service.
 */


d = i: debiki.internal, u: debiki.v0.util
$ = d.i.$;



/**
 * If you swipe right, in order to scroll left:
 * - Shows a "Left" button, which takes you to the left edge of the document.
 * - Shows an "Article" button, which takes you to the article (in the top left corner).
 *
 * If you swipe down, in order to scroll up:
 * - Shows "Prev", "Next" buttons, which takes you to the top of the previous
 *   and next column.
 * - Shows a "Top" button, which takes you to the top of the current column.
 */
d.i.enableSwipeGestures = !->

  options =
    prevent_default: false
    stop_browser_behavior: false
    swipe_velocity: 0

  hammer = Hammer($('body')[0], options)
  hammer.on('swipe', removeAnyShortcutbutton)

  if Modernizr.touch
    hammer
      .on('swiperight', handleSwipeRight)
      .on('swipedown', handleSwipeDown)
  else
    # Invert for desktop
    hammer
      .on('swipeleft', handleSwipeRight)
      .on('swipeup', handleSwipeDown)



!function removeAnyShortcutbutton(event)
  $('.dw-gesture-btn').remove()



!function handleSwipeRight(event)
  [leftBtnPos, articleBtnPos] =
    if Modernizr.touch => ['southeast', 'south']
    else ['east', 'northeast']
  makeShortcutButton(event, 'Left', leftBtnPos, scrollToLeftEdge)
  makeShortcutButton(event, 'Article', articleBtnPos, scrollToArticle)



!function handleSwipeDown(event)
  makeShortcutButton(event, 'Up', 'south', scrollToTopOfColumn)
  makeShortcutButton(event, 'Next', 'southeast', scrollToNextColumn)
  makeShortcutButton(event, 'Prev', 'southwest', scrollToPrevColumn)



function makeShortcutButton(event, text, position, onClick)
  button = $("""<a class="btn btn-default btn-lg dw-gesture-btn">#text</a>""")
  $('body').append(button)
  button.css(right: 0, bottom: 0)

  buttonWidth = button.outerWidth!
  buttonHeight = button.outerHeight!

  # $(window).width() gives wrong result in my Andorid browser if zoomed in/out.
  # This works however, both with my Android, and Chrome Desktop:
  winWidth = button.offset!left - $(window).scrollLeft! + buttonWidth
  winHeight = button.offset!top - $(window).scrollTop! + buttonHeight

  [top, left] = switch position
  | 'northeast' => [0,                              winWidth - buttonWidth      ]
  | 'east'      => [(winHeight - buttonHeight) / 2, winWidth - buttonWidth      ]
  | 'south'     => [winHeight - buttonHeight,       (winWidth - buttonWidth) / 2]
  | 'southeast' => [winHeight - buttonHeight,       winWidth - buttonWidth      ]
  | 'southwest' => [winHeight - buttonHeight,       0                           ]

  button.css(top: top, left: left, right: 'auto', bottom: 'auto')
  button.click !(event) ->
    removeAnyShortcutbutton()
    onClick(event)



!function scrollToLeftEdge
  $('html, body').animate({ 'scrollLeft': 0 }, 1500, 'swing')



!function scrollToArticle
  $('.dw-page').dwScrollIntoView(duration: 1600)



!function scrollToTopOfColumn
  ''



!function scrollToNextColumn
  firstToTheRight = void
  rootPostReplies = $('.dw-page > .dw-ar-t > .dw-res > li > .dw-t > .dw-p')
  windowRightEdge = $(window).scrollLeft! + $(window).width!
  rootPostReplies.each !->
    reply = $(this)
    rightEdge = reply.offset!left + reply.outerWidth!
    if windowRightEdge < rightEdge && !firstToTheRight
      firstToTheRight := reply

  if firstToTheRight
    column = firstToTheRight.parent!parent!
    # First scroll as much as possible of the whole column into view, then scroll
    # again for some minor adjustments.
    column.dwScrollIntoView(duration: 900)
    firstToTheRight.dwScrollIntoView(
        marginTop: 130 # leave some marginTop for the minimap
        marginLeft: 0
        marginRight: 150 # or scrolls too much to the left for some reason
        duration: 150)



!function scrollToPrevColumn
  ''



# vim: fdm=marker et ts=2 sw=2 fo=tcqwn list
