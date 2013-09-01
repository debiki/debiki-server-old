/* Shows a minimap of the discussion; a rectangle marks current viewport.
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
 * The minimap scrollfixes itself in the upper right corner. I couldn't
 * use any separate modular scroollfix directive for this, because
 * the scrollfix functionality needs to know if the user is currently
 * scrolling — then it's not a good idea to suddenly change the position
 * of the underlying map?
 *
 * Regrettably this code doesn't totally followed AngularJS best practises,
 * e.g. I'm using `document` directly.
 */


d = i: debiki.internal, u: debiki.v0.util
$ = d.i.$;



# `numComments` relied on below doesn't yet exist, so for now:
# (and don't count page title and body)
if $('.dw-p').length - 2 <= 5
  return


# If the minimap won't work anyway, never create it (leave the parent
# <div dw-minimap> empty).
if !Modernizr.canvas || !Modernizr.csspositionfixed
  return


DebikiPageModule = angular.module 'DebikiPageModule'


DebikiPageModule.directive 'dwMinimap', ['$window', dwMinimap]



# for now
winWidth = window.innerWidth
aspectRatio = document.width / (Math.max document.height, 1)
minimapWidth = Math.min(500, document.width / 20)
minimapHeight = minimapWidth / aspectRatio


function dwMinimap ($window)
  template: """
    <canvas
      id="dw-minimap"
      xx-ng-show="numComments > 0"
      width="#minimapWidth"
      height="#minimapHeight"
      ng-mousedown="startScrolling($event)">
    </canvas>
    """

  link: !(scope, elem, attrs) ->
    # Here we'll do 3 things:
    # - Draw and update the minimap.
    # - Handle mouse click and drag in the minimap — then move the viewport.
    # - Scrollfix the minimap in the upper right corner.

    # ----- Draw minimap

    canvas = elem.children('canvas')
    context = canvas[0].getContext '2d'
    context.fillStyle = '#666'

    # The article's .dw-p is very wide; use .dw-p-bd-blk instead.
    $('.dw-p-bd-blk').each !->
      drawPost($(this), context, minimapWidth, minimapHeight)

    cachedMinimap = context.getImageData(0, 0, minimapWidth, minimapHeight)

    angular.element($window).on 'scroll', !->
      context.clearRect(0, 0, minimapWidth, minimapHeight)
      context.putImageData(cachedMinimap, 0, 0)
      drawViewport(context, minimapWidth, minimapHeight)

    # ----- Handle click and drag in minimap

    isScrolling = false

    preventDefault = -> false

    scope.startScrolling = ($event) ->
      isScrolling := true
      $(window).on('mousemove', scope.scroll)
      $(window).on('mouseup', scope.stopScrolling)
      $(window).on('mouseleave', scope.stopScrolling)
      scope.scroll($event)

    scope.stopScrolling = ($event) ->
      $event.preventDefault()
      isScrolling := false
      $(window).off('mousemove', scope.scroll)
      $(window).off('mouseup', scope.stopScrolling)
      $(window).off('mouseleave', scope.stopScrolling)

    scope.scroll = ($event) ->
      if !isScrolling => return
      $event.preventDefault()
      canvasOffset = canvas.offset!
      docPosClickedX = ($event.pageX - canvasOffset.left) / minimapWidth * document.width
      docPosClickedY = ($event.pageY - canvasOffset.top) / minimapHeight * document.height
      newDocCornerX = docPosClickedX - window.innerWidth / 2
      newDocCornerY = docPosClickedY - window.innerHeight / 2
      window.scrollTo(newDocCornerX, newDocCornerY)

    # ----- Scrollfix the minimap

    # Adding 100 makes the minimap less jerky when scrolling mostly straight
    # down (which is the first thing people tend to do), and also takes
    # scrollbars into account.
    scrollfixOffset =
      x: canvas.offset!left + canvas.width! + 100
      y: canvas.offset!top

    angular.element($window).bind 'scroll', !->
      # This (pageYOffset) won't work in IE8, but Canvas isn't supported in IE8
      # anyway so this is dead code, in IE8.
      offsetY = $window.pageYOffset;
      offsetX = $window.pageXOffset;
      offsetXRightEdge = offsetX + $window.innerWidth
      if !(canvas.hasClass 'dw-scrollfix')
        if offsetY > scrollfixOffset.y || offsetXRightEdge > scrollfixOffset.x
          canvas.addClass 'dw-scrollfix'
      else if canvas.hasClass 'dw-scrollfix'
        if offsetY < scrollfixOffset.y && offsetXRightEdge < scrollfixOffset.x
          canvas.removeClass 'dw-scrollfix'




!function drawPost(bodyBlock, context, minimapWidth, minimapHeight)
  offset = bodyBlock.offset!
  height = bodyBlock.height!
  width = bodyBlock.width!
  x = minimapWidth * offset.left / document.width
  w = minimapWidth * width / document.width
  y = minimapHeight * offset.top / document.height
  h = minimapHeight * height / document.height
  # Make very short comments visiible by setting min size.
  w = Math.max 3, w
  h = Math.max 1, h
  context.fillRect(x, y, w, h)



!function drawViewport(context, minimapWidth, minimapHeight)
  x = minimapWidth * window.scrollX / document.width
  w = minimapWidth * window.innerWidth / document.width
  y = minimapHeight * window.scrollY / document.height
  h = minimapHeight * window.innerHeight / document.height

  context.beginPath()
  context.rect(x, y, w, h)
  context.stroke()



# vim: fdm=marker et ts=2 sw=2 tw=80 fo=tcqwn list
