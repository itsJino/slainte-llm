import { useRef, useEffect } from 'react';

const useAutoScroll = () => {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Find the nearest scrollable parent
    const findScrollableParent = (element: HTMLElement | null): HTMLElement | null => {
      if (!element) return null;
      
      // Check if element is scrollable
      const hasScrollableContent = element.scrollHeight > element.clientHeight;
      const overflowYStyle = window.getComputedStyle(element).overflowY;
      const isScrollable = overflowYStyle !== 'visible' && overflowYStyle !== 'hidden';
      
      if (hasScrollableContent && isScrollable) {
        return element;
      }
      
      // If not, check its parent
      return findScrollableParent(element.parentElement);
    };

    // Scroll to bottom of content
    if (ref.current) {
      // First try to scroll the element itself
      if (ref.current.scrollHeight > ref.current.clientHeight) {
        ref.current.scrollTop = ref.current.scrollHeight;
      } else {
        // Otherwise find and scroll the nearest scrollable parent
        const scrollableParent = findScrollableParent(ref.current);
        if (scrollableParent) {
          scrollableParent.scrollTop = scrollableParent.scrollHeight;
        }
      }
    }
  });

  return ref;
};

export default useAutoScroll;