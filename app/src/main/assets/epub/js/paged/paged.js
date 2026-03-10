let shadowRoot;
let cachedColumnWidth = null;
let currentPage = 0;
let totalPages = 0;

function initShadow() {
  const host = document.getElementById('content');
  shadowRoot = host.attachShadow({ mode: 'open' });

  const link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = 'epub/css/paged/paged.css';
  shadowRoot.appendChild(link);

  const pager = document.createElement('div');
  pager.id = 'pager';
  shadowRoot.appendChild(pager);

  function updateSizes(){
    const w = window.innerWidth;
    const h = window.innerHeight;
    shadowRoot.host.style.setProperty('--vw',`${w}px`);
    shadowRoot.host.style.setProperty('--vh',`${h}px`);
  }

  updateSizes();

  window.addEventListener('resize',()=>{
    cachedColumnWidth=null;
    updateSizes();
    calculateTotalPages();
  });

  initSwipeGesture();
}

function initSwipeGesture(){
  const host = shadowRoot.host;
  let startX=0;
  let startTime=0;
  let dragging=false;

  const THRESHOLD=0.2;
  const VELOCITY_THRESHOLD=0.3;

  host.addEventListener('touchstart',(e)=>{
    startX=e.touches[0].clientX;
    startTime=Date.now();
    dragging=true;
  },{passive:true});

  host.addEventListener('touchmove',(e)=>{
    if(!dragging) return;

    const pager=shadowRoot.getElementById('pager');
    const colWidth=getRealColumnWidth();

    const deltaX=e.touches[0].clientX-startX;

    if((deltaX>0 && currentPage===0) || (deltaX<0 && currentPage===totalPages-1)) return;

    pager.style.transition='none';
    pager.style.transform=`translateX(${-currentPage*colWidth+deltaX}px)`;

  },{passive:true});

  host.addEventListener('touchend',(e)=>{
    if(!dragging) return;
    dragging=false;

    const pager=shadowRoot.getElementById('pager');
    const colWidth=getRealColumnWidth();

    const deltaX=e.changedTouches[0].clientX-startX;
    const velocity=Math.abs(deltaX)/(Date.now()-startTime);

    const isSwipeLeft  = deltaX<0 && (Math.abs(deltaX)>colWidth*THRESHOLD || velocity>VELOCITY_THRESHOLD);
    const isSwipeRight = deltaX>0 && (Math.abs(deltaX)>colWidth*THRESHOLD || velocity>VELOCITY_THRESHOLD);

    pager.style.transition='transform .3s ease';

    if(isSwipeLeft && currentPage<totalPages-1) goToPage(currentPage+1);
    else if(isSwipeRight && currentPage>0) goToPage(currentPage-1);
    else goToPage(currentPage);

    setTimeout(()=>{pager.style.transition='none';},300);

  },{passive:true});
}

async function loadContent(chaptersJson){
  const chapters=JSON.parse(chaptersJson);
  const pager=shadowRoot.getElementById('pager');

  chapters.forEach(ch => {
    const section=document.createElement('section');
    section.id=ch.id;
    section.innerHTML=decodeB64(ch.html);
    pager.appendChild(section);
  });

  cachedColumnWidth=null;
  await waitForImagesAndFonts(shadowRoot);

  requestAnimationFrame(()=>{
    calculateTotalPages();
  });
}

function goToPage(page){
  const pager=shadowRoot.getElementById('pager');
  if(!pager) return;

  const newPage=Math.max(0,page);
  const colWidth=getRealColumnWidth();
  pager.style.transform=`translateX(${-newPage*colWidth}px)`;

  if(newPage!==currentPage){
    currentPage=newPage;
    bridge.onPageChanged(currentPage+1,totalPages);
  }else{
    currentPage=newPage;
  }
}

function navigateToId(id){
  const pager=shadowRoot.getElementById('pager');
  const el=shadowRoot.getElementById(id);

  if(!pager || !el) return;

  const colWidth=getRealColumnWidth();

  let offset=0;
  let node=el;

  while(node && node!==pager){
    offset+=node.offsetLeft;
    node=node.offsetParent;
  }

  const page=Math.floor((offset+colWidth/2)/colWidth);

  goToPage(page);

}

function getTotalPages(){
  const pager=shadowRoot.getElementById('pager');
  if(!pager) return 0;

  const savedTransform=pager.style.transform;
  pager.style.transform='none';

  const colWidth=pager.getBoundingClientRect().width;
  const scrollWidth=pager.scrollWidth;
  pager.style.transform=savedTransform;
  cachedColumnWidth=colWidth;

  const EPS=0.5;
  const pages=Math.max(1,Math.ceil((scrollWidth-EPS)/colWidth));

  return pages;
}

function calculateTotalPages(){
  totalPages=getTotalPages();
  bridge.onPagesCalculated(totalPages);
}

function getRealColumnWidth(){
  if(cachedColumnWidth) return cachedColumnWidth;
  const pager=shadowRoot.getElementById('pager');
  const currentTransform=pager.style.transform;
  pager.style.transform='none';

  const width=pager.getBoundingClientRect().width;
  pager.style.transform=currentTransform;

  cachedColumnWidth = width>0
    ? width
    : parseFloat(shadowRoot.host.style.getPropertyValue('--vw'));

  return cachedColumnWidth;
}

function init() {
  initShadow();
  setupNavigationHandler(shadowRoot);
}

document.addEventListener('DOMContentLoaded', init);
